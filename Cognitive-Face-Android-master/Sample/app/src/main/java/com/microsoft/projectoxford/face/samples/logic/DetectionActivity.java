package com.microsoft.projectoxford.face.samples.logic;

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
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
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
import com.microsoft.projectoxford.face.contract.Accessory;
import com.microsoft.projectoxford.face.contract.Hair;
import com.microsoft.projectoxford.face.contract.Makeup;
import com.microsoft.projectoxford.face.samples.R;
import com.microsoft.projectoxford.face.samples.helper.ImageHelper;
import com.microsoft.projectoxford.face.samples.helper.SampleApp;
import com.microsoft.projectoxford.face.samples.ui.SelectImageActivity;

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
import java.util.Objects;

public class DetectionActivity extends AppCompatActivity {

    MediaPlayer mPlayer;
    Button startButton, pauseButton, stopButton;
    Map<String, String> map = new HashMap<String, String>();

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
                        params[0],
                        true,
                        true,

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
                return null;
            }
        }

        @Override
        protected void onPreExecute() {
            mProgressDialog.show();
        }

        @Override
        protected void onProgressUpdate(String... progress) {
            mProgressDialog.setMessage(progress[0]);
            setInfo(progress[0]);
        }

        @Override
        protected void onPostExecute(Face[] result) {
            if (mSucceed) {
                setUiAfterDetection(result, mSucceed);
            }


        }
    }


    private static final int REQUEST_SELECT_IMAGE = 0;

    private Uri mImageUri;


    private Bitmap mBitmap;


    ProgressDialog mProgressDialog;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_detection);

        mProgressDialog = new ProgressDialog(this);
        mProgressDialog.setTitle(getString(R.string.progress_dialog_title));


        setDetectButtonEnabledStatus(false);


        startButton = findViewById(R.id.btnPlay);
        pauseButton = findViewById(R.id.btnPause);
        stopButton = findViewById(R.id.btnStop);
        startButton.setVisibility(View.INVISIBLE);
        pauseButton.setVisibility(View.INVISIBLE);
        stopButton.setVisibility(View.INVISIBLE);
    }

    private void musicOn(String emotionType){

        map.put("Веселый:)", Objects.requireNonNull(getMusic("Веселый:)")));
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
            Toast.makeText(this,"Подбираем музыку!", Toast.LENGTH_SHORT).show();
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

        @Override
    public void onDestroy() {
        super.onDestroy();
        if (mPlayer.isPlaying()) {
            stopPlay();
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putParcelable("ImageUri", mImageUri);
    }

    @Override
    protected void onRestoreInstanceState(@NonNull Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);

        mImageUri = savedInstanceState.getParcelable("ImageUri");
        if (mImageUri != null) {
            mBitmap = ImageHelper.loadSizeLimitedBitmapFromUri(
                    mImageUri, getContentResolver());
        }
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case REQUEST_SELECT_IMAGE:
                if (resultCode == RESULT_OK) {

                    mImageUri = data.getData();
                    mBitmap = ImageHelper.loadSizeLimitedBitmapFromUri(
                            mImageUri, getContentResolver());
                    if (mBitmap != null) {

                        ImageView imageView = findViewById(R.id.image);
                        imageView.setImageBitmap(mBitmap);


                    }


                    FaceListAdapter faceListAdapter = new FaceListAdapter(null);
                    ListView listView = findViewById(R.id.list_detected_faces);
                    listView.setAdapter(faceListAdapter);

                    setInfo("");

                    setDetectButtonEnabledStatus(true);
                }
                break;
            default:
                break;
        }
    }


    public void selectImage(View view) {
        Intent intent = new Intent(this, SelectImageActivity.class);
        startActivityForResult(intent, REQUEST_SELECT_IMAGE);
    }


    public void detect(View view) {

        ByteArrayOutputStream output = new ByteArrayOutputStream();
        mBitmap.compress(Bitmap.CompressFormat.JPEG, 100, output);
        ByteArrayInputStream inputStream = new ByteArrayInputStream(output.toByteArray());

        new DetectionTask().execute(inputStream);

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
        Button detectButton = findViewById(R.id.detect);
        detectButton.setEnabled(isEnabled);
    }

    // Set whether the buttons are enabled.
    private void setAllButtonsEnabledStatus(boolean isEnabled) {
        Button selectImageButton =  findViewById(R.id.select_image);
        selectImageButton.setEnabled(isEnabled);

        Button detectButton =  findViewById(R.id.detect);
        detectButton.setEnabled(isEnabled);


    }

    // Set the information panel on screen.
    private void setInfo(String info) {
        TextView textView = findViewById(R.id.info);
        textView.setText(info);
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
                    musicOn(emotionType);
                    break;
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
