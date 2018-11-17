//
// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license.
//
// Microsoft Cognitive Services (formerly Project Oxford): https://www.microsoft.com/cognitive-services
//
// Microsoft Cognitive Services (formerly Project Oxford) GitHub:
// https://github.com/Microsoft/Cognitive-Face-Android
//
// Copyright (c) Microsoft Corporation
// All rights reserved.
//
// MIT License:
// Permission is hereby granted, free of charge, to any person obtaining
// a copy of this software and associated documentation files (the
// "Software"), to deal in the Software without restriction, including
// without limitation the rights to use, copy, modify, merge, publish,
// distribute, sublicense, and/or sell copies of the Software, and to
// permit persons to whom the Software is furnished to do so, subject to
// the following conditions:
//
// The above copyright notice and this permission notice shall be
// included in all copies or substantial portions of the Software.
//
// THE SOFTWARE IS PROVIDED ""AS IS"", WITHOUT WARRANTY OF ANY KIND,
// EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
// MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
// NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE
// LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION
// OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION
// WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
//
package com.microsoft.projectoxford.face.samples.ui;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.microsoft.projectoxford.face.FaceServiceClient;
import com.microsoft.projectoxford.face.contract.Emotion;
import com.microsoft.projectoxford.face.contract.Face;
import com.microsoft.projectoxford.face.contract.FacialHair;
import com.microsoft.projectoxford.face.contract.HeadPose;
import com.microsoft.projectoxford.face.contract.Accessory;
import com.microsoft.projectoxford.face.contract.Blur;
import com.microsoft.projectoxford.face.contract.Exposure;
import com.microsoft.projectoxford.face.contract.Hair;
import com.microsoft.projectoxford.face.contract.Makeup;
import com.microsoft.projectoxford.face.contract.Noise;
import com.microsoft.projectoxford.face.contract.Occlusion;
import com.microsoft.projectoxford.face.samples.R;
import com.microsoft.projectoxford.face.samples.helper.ImageHelper;
import com.microsoft.projectoxford.face.samples.helper.LogHelper;
import com.microsoft.projectoxford.face.samples.helper.SampleApp;
import com.microsoft.projectoxford.face.samples.log.DetectionLogActivity;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DetectionActivity extends AppCompatActivity implements MediaPlayer.OnPreparedListener {

    MediaPlayer mPlayer;
    Button startButton, pauseButton, stopButton;
    Map<String, String> map = new HashMap<String, String>();

    String[] faces = new String[100];



//    List final String DATA_HTTP  = "https://audio-ssl.itunes.apple.com/apple-assets-us-std-000001/AudioPreview128/v4/18/00/83/180083c4-e8d1-8edd-080d-2d4f8c39f6d7/mzaf_8475248627883495306.plus.aac.p.m4a";
//    final String DATA_HTTP2  = "https://www.europaplus.ru/sound/1524155304_Calvin_Harris__Dua_Lipa_-_One_Kiss.mp3";



    // Background task of face detection.
    private class DetectionTask extends AsyncTask<InputStream, String, Face[]> {
        private boolean mSucceed = true;



        @Override
        protected Face[] doInBackground(InputStream... params) {
            // Get an instance of face service client to detect faces in image.
            FaceServiceClient faceServiceClient = SampleApp.getFaceServiceClient();
            try {
                publishProgress("Определяем...");

                // Start detection.
                return faceServiceClient.detect(
                        params[0],  /* Input stream of image to detect */
                        true,       /* Whether to return face ID */
                        true,       /* Whether to return face landmarks */
                        /* Which face attributes to analyze, currently we support:
                           age,gender,headPose,smile,facialHair */
                        new FaceServiceClient.FaceAttributeType[] {
                                FaceServiceClient.FaceAttributeType.Age,
                                FaceServiceClient.FaceAttributeType.Gender,
                                FaceServiceClient.FaceAttributeType.Smile,
                                FaceServiceClient.FaceAttributeType.Glasses,
                                FaceServiceClient.FaceAttributeType.FacialHair,
                                FaceServiceClient.FaceAttributeType.Emotion,
                                FaceServiceClient.FaceAttributeType.HeadPose,
                                FaceServiceClient.FaceAttributeType.Accessories,
                                FaceServiceClient.FaceAttributeType.Blur,
                                FaceServiceClient.FaceAttributeType.Exposure,
                                FaceServiceClient.FaceAttributeType.Hair,
                                FaceServiceClient.FaceAttributeType.Makeup,
                                FaceServiceClient.FaceAttributeType.Noise,
                                FaceServiceClient.FaceAttributeType.Occlusion
                        });
            } catch (Exception e) {
                mSucceed = false;
                publishProgress(e.getMessage());
                addLog(e.getMessage());
                return null;
            }
        }

        @Override
        protected void onPreExecute() {
            mProgressDialog.show();
            addLog("Request: Detecting in image " + mImageUri);
        }

        @Override
        protected void onProgressUpdate(String... progress) {
            mProgressDialog.setMessage(progress[0]);
            setInfo(progress[0]);
        }

        @Override
        protected void onPostExecute(Face[] result) {
            if (mSucceed) {
                addLog("Response: Success. Detected " + (result == null ? 0 : result.length)
                        + " face(s) in " + mImageUri);
            }

            // Show the result on screen when detection is done.
            setUiAfterDetection(result, mSucceed);
        }
    }

    // Flag to indicate which task is to be performed.
    private static final int REQUEST_SELECT_IMAGE = 0;

    // The URI of the image selected to detect.
    private Uri mImageUri;

    // The image selected to detect.
    private Bitmap mBitmap;

    // Progress dialog popped up when communicating with server.
    ProgressDialog mProgressDialog;

    // When the activity is created, set all the member variables to initial state.
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_detection);

        mProgressDialog = new ProgressDialog(this);
        mProgressDialog.setTitle(getString(R.string.progress_dialog_title));




        // Disable button "detect" as the image to detect is not selected.
        setDetectButtonEnabledStatus(false);

        LogHelper.clearDetectionLog();

        startButton = findViewById(R.id.btnPlay);
        pauseButton = findViewById(R.id.btnPause);
        stopButton = findViewById(R.id.btnStop);
        startButton.setVisibility(View.INVISIBLE);
        pauseButton.setVisibility(View.INVISIBLE);
        stopButton.setVisibility(View.INVISIBLE);




//        try {
//            mPlayer.setDataSource(DATA_HTTP);
//            mPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
//
//            mPlayer.setOnPreparedListener(this);
//            mPlayer.prepareAsync();
//        } catch (IOException e) {
//            e.printStackTrace();
//        }


//        mPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
//            @Override
//            public void onCompletion(MediaPlayer mp) {
//                stopPlay();
//            }
//        });

    }

    private void musicOn(String emotionType){


//        map.put("Neutral", "https://www.europaplus.ru/sound/1524155304_Calvin_Harris__Dua_Lipa_-_One_Kiss.mp3");
//        map.put("Sadness", "http://ol6.mp3party.net/online/8465/8465102.mp3");
//        map.put("Anger", "  http://ol1.mp3party.net/online/49/49449.mp3");
//        map.put("Happiness""https://audio-ssl.itunes.apple.com/apple-assets-us-std-000001/AudioPreview128/v4/18/00/83/180083c4-e8d1-8edd-080d-2d4f8c39f6d7/mzaf_8475248627883495306.plus.aac.p.m4a");
        map.put("Веселый:)", getMusic("Веселый:)"));
        map.put("Злой", getMusic("Злой"));
        map.put("Обычный)", getMusic("Обычный)"));
        map.put("Грустный:(", getMusic("Грустный:("));
        map.put("Удивленный", getMusic("Удивленный"));



        startButton.setVisibility(View.VISIBLE);
        pauseButton.setVisibility(View.VISIBLE);
        stopButton.setVisibility(View.VISIBLE);
        try {
            if(mPlayer!=null)
                stopPlay();
            mPlayer = new MediaPlayer();
            mPlayer.setDataSource(map.get(emotionType));
            mPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);

            mPlayer.setOnPreparedListener(this);
            mPlayer.prepareAsync();
        } catch (IOException e) {
            e.printStackTrace();
        }


        mPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mp) {
                stopPlay();
            }
        });
    }

    private String getMusic(String emotionType){
        Resources res = getResources();
        String[] mas;
        switch (emotionType){
            case "Веселый:)":
                mas = res.getStringArray(R.array.songsHap);
                return mas[(int) (Math.random() * 6)];
            case "Злой":
                mas = res.getStringArray(R.array.songsAngr);
                return mas[(int) (Math.random() * 4)];
            case "Обычный)":
                mas = res.getStringArray(R.array.songsNeutr);
                return mas[(int) (Math.random() * 6)];
            case "Грустный:(":
                mas = res.getStringArray(R.array.songsSad);
                return mas[(int) (Math.random() * 5)];
            case "Удивленный":
                mas = res.getStringArray(R.array.songsSurp);
                return mas[(int) (Math.random() * 5)];
            default: return null;

        }
    }

    private void stopPlay(){
        mPlayer.stop();
        pauseButton.setEnabled(false);
        stopButton.setEnabled(false);
        try {
            mPlayer.prepare();
            mPlayer.seekTo(0);
            startButton.setEnabled(true);
        }
        catch (Throwable t) {
            Toast.makeText(this, t.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }


    public void play(View view){

        mPlayer.start();
        startButton.setEnabled(false);
        pauseButton.setEnabled(true);
        stopButton.setEnabled(true);
    }

    public void pause(View view){

        mPlayer.pause();
        startButton.setEnabled(true);
        pauseButton.setEnabled(false);
        stopButton.setEnabled(true);
    }
    public void stop(View view){
        stopPlay();
    }

//    @Override
//    public void onDestroy() {
//        super.onDestroy();
//        if (mPlayer.isPlaying()) {
//            stopPlay();
//        }
//    }



    @Override
    public void onPrepared(MediaPlayer mediaPlayer) {

    }


    // Save the activity state when it's going to stop.
    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putParcelable("ImageUri", mImageUri);
    }

    // Recover the saved state when the activity is recreated.
    @Override
    protected void onRestoreInstanceState(@NonNull Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);

        mImageUri = savedInstanceState.getParcelable("ImageUri");
        if (mImageUri != null) {
            mBitmap = ImageHelper.loadSizeLimitedBitmapFromUri(
                    mImageUri, getContentResolver());
        }
    }

    // Called when image selection is done.
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case REQUEST_SELECT_IMAGE:
                if (resultCode == RESULT_OK) {
                    // If image is selected successfully, set the image URI and bitmap.
                    mImageUri = data.getData();
                    mBitmap = ImageHelper.loadSizeLimitedBitmapFromUri(
                            mImageUri, getContentResolver());
                    if (mBitmap != null) {
                        // Show the image on screen.
                        ImageView imageView = findViewById(R.id.image);
                        imageView.setImageBitmap(mBitmap);

                        // Add detection log.
                        addLog("Image: " + mImageUri + " resized to " + mBitmap.getWidth()
                                + "x" + mBitmap.getHeight());
                    }


                    // Clear the detection result.
                    FaceListAdapter faceListAdapter = new FaceListAdapter(null);
                    ListView listView = findViewById(R.id.list_detected_faces);
                    listView.setAdapter(faceListAdapter);


                    // Clear the information panel.
                    setInfo("");

                    // Enable button "detect" as the image is selected and not detected.
                    setDetectButtonEnabledStatus(true);
                }
                break;
            default:
                break;
        }
    }

    // Called when the "Select Image" button is clicked.
    public void selectImage(View view) {
        Intent intent = new Intent(this, SelectImageActivity.class);
        startActivityForResult(intent, REQUEST_SELECT_IMAGE);
    }

    // Called when the "Detect" button is clicked.
    public void detect(View view) {
        // Put the image into an input stream for detection.
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        mBitmap.compress(Bitmap.CompressFormat.JPEG, 100, output);
        ByteArrayInputStream inputStream = new ByteArrayInputStream(output.toByteArray());

        // Start a background task to detect faces in the image.
        new DetectionTask().execute(inputStream);

        // Prevent button click during detecting.
        setAllButtonsEnabledStatus(false);
    }



    // Show the result on screen when detection is done.
    private void setUiAfterDetection(Face[] result, boolean succeed) {
        // Detection is done, hide the progress dialog.
        mProgressDialog.dismiss();

        // Enable all the buttons.
        setAllButtonsEnabledStatus(true);

        // Disable button "detect" as the image has already been detected.
        setDetectButtonEnabledStatus(false);

        if (succeed) {
            // The information about the detection result.
            String detectionResult;
            if (result != null) {
                detectionResult = result.length + " face"
                        + (result.length != 1 ? "s" : "") + " detected";

                // Show the detected faces on original image.
                ImageView imageView = (ImageView) findViewById(R.id.image);
                imageView.setImageBitmap(ImageHelper.drawFaceRectanglesOnBitmap(
                        mBitmap, result, true));

                // Set the adapter of the ListView which contains the details of the detected faces.
                FaceListAdapter faceListAdapter = new FaceListAdapter(result);

                // Show the detailed list of detected faces.
                ListView listView = (ListView) findViewById(R.id.list_detected_faces);
                listView.setAdapter(faceListAdapter);
            } else {
                detectionResult = "0 face detected";
            }
            setInfo(detectionResult);
        }

        mImageUri = null;
        mBitmap = null;
    }

    // Set whether the buttons are enabled.
    private void setDetectButtonEnabledStatus(boolean isEnabled) {
        Button detectButton = (Button) findViewById(R.id.detect);
        detectButton.setEnabled(isEnabled);
    }

    // Set whether the buttons are enabled.
    private void setAllButtonsEnabledStatus(boolean isEnabled) {
        Button selectImageButton = (Button) findViewById(R.id.select_image);
        selectImageButton.setEnabled(isEnabled);

        Button detectButton = (Button) findViewById(R.id.detect);
        detectButton.setEnabled(isEnabled);


    }

    // Set the information panel on screen.
    private void setInfo(String info) {
        TextView textView = (TextView) findViewById(R.id.info);
        textView.setText(info);
    }

    // Add a log item.
    private void addLog(String log) {
        LogHelper.addDetectionLog(log);
    }

    // The adapter of the GridView which contains the details of the detected faces.
    private class FaceListAdapter extends BaseAdapter {
        // The detected faces.
        List<Face> faces;

        // The thumbnails of detected faces.
        List<Bitmap> faceThumbnails;

        // Initialize with detection result.
        FaceListAdapter(Face[] detectionResult) {
            faces = new ArrayList<>();
            ListView listView = findViewById(R.id.list_detected_faces);
            faceThumbnails = new ArrayList<>();
            if (detectionResult != null) {
                faces = Arrays.asList(detectionResult);
                for (Face face : faces) {
                    try {
                        // Crop face thumbnail with five main landmarks drawn from original image.
                        faceThumbnails.add(ImageHelper.generateFaceThumbnail(
                                mBitmap, face.faceRectangle));
                    } catch (IOException e) {
                        // Show the exception when generating face thumbnail fails.
                        setInfo(e.getMessage());
                    }
                }
            }

            listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {
                    FaceListAdapter clickedObj = (FaceListAdapter) adapterView.getItemAtPosition(position);
                    chooseEmotion(getEmotion(clickedObj.faces.get(position).faceAttributes.emotion));
                }
            });
        }

        @Override
        public boolean isEnabled(int position) {
            return false;
        }

        @Override
        public int getCount() {
            return faces.size();
        }

        @Override
        public Object getItem(int position) {
            return faces.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(final int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                LayoutInflater layoutInflater =
                        (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                convertView = layoutInflater.inflate(R.layout.item_face_with_description, parent, false);
            }
            convertView.setId(position);

            // Show the face thumbnail.
            ((ImageView) convertView.findViewById(R.id.face_thumbnail)).setImageBitmap(
                    faceThumbnails.get(position));

            // Show the face details.
            DecimalFormat formatter = new DecimalFormat("#0.0");
            String face_description = String.format("Возвраст: %s  \nПол: %s\nВолосы: %s  Щетина: %s\nМэйкап: %s\nНастроение:   %s\n" +
                            "\nАксессуары: %s",
                    faces.get(position).faceAttributes.age,
                    getGender(faces.get(position).faceAttributes.gender),
                    getHair(faces.get(position).faceAttributes.hair),
                    getFacialHair(faces.get(position).faceAttributes.facialHair),
                    getMakeup((faces.get(position)).faceAttributes.makeup),
                    getEmotion(faces.get(position).faceAttributes.emotion),
                    faces.get(position).faceAttributes.glasses,
                    getAccessories(faces.get(position).faceAttributes.accessories)
                    );
            ((TextView) convertView.findViewById(R.id.text_detected_face)).setText(face_description);

            return convertView;
        }

        private String getHair(Hair hair) {
            if (hair.hairColor.length == 0)
            {
                if (hair.invisible)
                    return "Невидимый";
                else
                    return "Лысый";
            }
            else
            {
                int maxConfidenceIndex = 0;
                double maxConfidence = 0.0;

                for (int i = 0; i < hair.hairColor.length; ++i)
                {
                    if (hair.hairColor[i].confidence > maxConfidence)
                    {
                        maxConfidence = hair.hairColor[i].confidence;
                        maxConfidenceIndex = i;
                    }
                }

                return getHairColor(hair.hairColor[maxConfidenceIndex].color.toString());
            }
        }

        private String getHairColor(String hairColor){
            switch(hairColor){
                case "White": return "Светлый";
                case "Brown": return "Коричневый";
                case "Gray": return "Серый";
                case "Blond": return "Блондин";
                case "Red": return "Красный";
                case "Black": return "Черный";
                case "Other": return "Другое";
                default: return "Другое";
            }
        }

        private String getGender(String gender){
            if(gender.equals("male")){
                    return "Мужской";
             }else {
                    return "Женский";
   }

        }

        private String getMakeup(Makeup makeup) {
            return  (makeup.eyeMakeup || makeup.lipMakeup) ? "Да" : "Нет" ;
        }

        private String getAccessories(Accessory[] accessories) {
            if (accessories.length == 0)
            {
                return "Аксессуров нет";
            }
            else
            {
                String[] accessoriesList = new String[accessories.length];
                for (int i = 0; i < accessories.length; ++i)
                {
                    accessoriesList[i] = accessories[i].type.toString();
                }

                return TextUtils.join(",", accessoriesList);
            }
        }

        private String getAccessoriesTranslate(String accessoriesList){
            switch (accessoriesList){
                case "Headwear": return "Головной убор";
                case "Mask": return "Маска";
                case "Glasses": return "Очки";
                default: return "Отсутвуют";
            }
        }

        private String getFacialHair(FacialHair facialHair) {
            return (facialHair.moustache + facialHair.beard + facialHair.sideburns > 0) ? "Да" : "Нет";
        }

        private String getEmotion(Emotion emotion)
        {
            String emotionType = "";
            double emotionValue = 0.0;
            if (emotion.anger > emotionValue)
            {
                emotionValue = emotion.anger;
                emotionType = "Злой";
            }

            if (emotion.happiness > emotionValue)
            {
                emotionValue = emotion.happiness;
                emotionType = "Веселый:)";
            }
            if (emotion.neutral > emotionValue)
            {
                emotionValue = emotion.neutral;
                emotionType = "Обычный)";
            }
            if (emotion.sadness > emotionValue)
            {
                emotionValue = emotion.sadness;
                emotionType = "Грустный:(";
            }
            if (emotion.surprise > emotionValue)
            {
                emotionValue = emotion.surprise;
                emotionType = "Удивленный";
            }
            chooseEmotion(emotionType);
            return String.format("%s: %f", emotionType, emotionValue);
        }

               private void chooseEmotion(String emotionType){
            switch (emotionType){
                case "Злой":
                    musicOn(emotionType);
                    break;
                case "Веселый:)":
                    musicOn(emotionType);
                    break;
                case "Обычный)":
                    musicOn(emotionType); break;
                case "Грустный:(":
                    musicOn(emotionType);
                    break;
                case "Удивленный":
                    musicOn(emotionType);
                    break;

                default:
                    Toast.makeText(getApplicationContext(),"Извините, мы не смогли определить ваше настроение!",Toast.LENGTH_LONG).show();

            }
        }
    }
}
