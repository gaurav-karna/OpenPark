package com.example.openpark;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.provider.MediaStore;
import android.view.View;
import android.widget.ImageView;

import com.example.openpark.MainActivity;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;

import com.google.firebase.ml.common.FirebaseMLException;
import com.google.firebase.ml.common.modeldownload.FirebaseModelDownloadConditions;
import com.google.firebase.ml.common.modeldownload.FirebaseModelManager;
import com.google.firebase.ml.custom.FirebaseCustomLocalModel;
import com.google.firebase.ml.custom.FirebaseCustomRemoteModel;
import com.google.firebase.ml.custom.FirebaseModelDataType;
import com.google.firebase.ml.custom.FirebaseModelInputOutputOptions;
import com.google.firebase.ml.custom.FirebaseModelInputs;
import com.google.firebase.ml.custom.FirebaseModelInterpreter;
import com.google.firebase.ml.custom.FirebaseModelInterpreterOptions;
import com.google.firebase.ml.custom.FirebaseModelOutputs;
import com.google.firebase.ml.vision.FirebaseVision;
import com.google.firebase.ml.vision.common.FirebaseVisionImage;
import com.google.firebase.ml.vision.text.FirebaseVisionText;
import com.google.firebase.ml.vision.text.FirebaseVisionTextRecognizer;

import java.io.FileNotFoundException;
import java.io.IOException;

public class AnalysisWait extends AppCompatActivity {

    // image to analyze URI
    private static Uri imageUri;

    // Name of ML model currently in use
    private static final String CURRENT_MODEL_NAME = "openpark_final_v1";

    // path to current model
    private static final String MODEL_PATH = "assets/model.tflite";

    // loading screen
    ProgressDialog loadingBox;

    /* Input and output types of current model
    [  1 512 512   3]
    <class 'numpy.uint8'>
    [ 1 40  4]
    <class 'numpy.float32'>
     */

    // variables to hold OCR and custom results
    FirebaseVisionText OCR_RESULTS;
    FirebaseModelOutputs CUSTOM_RESULTS;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_analysis_wait);

        Intent analysisIntent = getIntent();
        imageUri = Uri.parse(analysisIntent.getStringExtra("scaled_image"));

        ImageView iv = (ImageView) findViewById(R.id.croppedImage);
        iv.setImageURI(imageUri);
    }

    public void tryAgain(View view) {
//        MainActivity.getInstance().dispatchTakePictureIntent(view);
        finish();
    }

    public void analyzeImage(View view) {
        // take user to loading screen while algo runs...
        loadingBox = ProgressDialog.show(this, "Processing 1/2",
                "Analyzing image...", true);

        // get image to analyze and instantiate into FireBase object
        FirebaseVisionImage imageToAnalyze;
        try {
            imageToAnalyze = FirebaseVisionImage.fromFilePath(this, imageUri);
        } catch (IOException e) {
            e.printStackTrace();
            finish();
            return;
        }

        // get OCR object
        FirebaseVisionTextRecognizer ocrDetector = FirebaseVision.getInstance()
                .getOnDeviceTextRecognizer();

        // get custom ML model objectt


        // start analysis
        Task<FirebaseVisionText> textResult =
                ocrDetector.processImage(imageToAnalyze)
                        .addOnSuccessListener(new OnSuccessListener<FirebaseVisionText>() {
                            @Override
                            public void onSuccess(FirebaseVisionText firebaseVisionText) {
                                OCR_RESULTS = firebaseVisionText;
                                // kill old loading box
                                loadingBox.dismiss();
                                // update loading box (Stage 2, custom model)
                                loadingBox = ProgressDialog.show(AnalysisWait.this,
                                        "Processing 2/2", "Analyzing image...",
                                        true);

                                // FirebaseCustomRemoteModel inherits from FirebaseRemoteModel
                                final FirebaseCustomRemoteModel remoteModel =
                                        new FirebaseCustomRemoteModel.Builder(CURRENT_MODEL_NAME)
                                                .build();

                                // setting up Local fallback
                                final FirebaseCustomLocalModel localModel = new FirebaseCustomLocalModel.Builder()
                                        .setAssetFilePath(MODEL_PATH)
                                        .build();

                                // if no model in assets, then get from MLKit
                                FirebaseModelDownloadConditions conditions = new FirebaseModelDownloadConditions.Builder()
                                        .requireWifi()
                                        .build();
                                FirebaseModelManager.getInstance().download(remoteModel, conditions)
                                        .addOnCompleteListener(new OnCompleteListener<Void>() {
                                            @Override
                                            public void onComplete(@NonNull Task<Void> task) {
                                                return;     // updated model if needed.
                                            }
                                        });

                                // checking if model is downloaded, and creating interpreter
                                FirebaseModelManager.getInstance().isModelDownloaded(remoteModel)
                                        .addOnSuccessListener(new OnSuccessListener<Boolean>() {
                                            @Override
                                            public void onSuccess(Boolean isDownloaded) {
                                                // choose remote or local based on download
                                                FirebaseModelInterpreterOptions options;
                                                if (isDownloaded) {
                                                    options = new FirebaseModelInterpreterOptions.Builder(remoteModel).build();
                                                } else {
                                                    options = new FirebaseModelInterpreterOptions.Builder(localModel).build();
                                                }
                                                try {
                                                    FirebaseModelInterpreter mlInterpreter =
                                                            FirebaseModelInterpreter.getInstance(options);
                                                    FirebaseModelInputOutputOptions inputOutputOptions =
                                                            new FirebaseModelInputOutputOptions.Builder()
                                                                    .setInputFormat(0,
                                                                            FirebaseModelDataType.INT32,
                                                                            new int[]{1, 512, 512, 3})
                                                                    .setOutputFormat(0,
                                                                            FirebaseModelDataType.FLOAT32,
                                                                            new int[]{1, 40, 4})
                                                                    .build();

                                                    // building input of image to analyze
                                                    int[][][][] modelInput = getScaledImageValue();
                                                    // fault tolerance
                                                    if (modelInput == null) {
                                                        loadingBox.dismiss();
                                                        loadingBox = ProgressDialog.show(AnalysisWait.this,
                                                                "Failure Encountered",
                                                                "Input creation failed, returning to home...",
                                                                true);
                                                        // delay of 1.5 seconds post failure
                                                        timerDelayRemoveDialog(1500, loadingBox);
                                                        finish();
                                                    }

                                                    // passing inputs and running the model
                                                    FirebaseModelInputs inputs = new FirebaseModelInputs.Builder()
                                                            .add(modelInput)
                                                            .build();
                                                    mlInterpreter.run(inputs, inputOutputOptions)
                                                            .addOnSuccessListener(
                                                                    new OnSuccessListener<FirebaseModelOutputs>() {
                                                                        @Override
                                                                        public void onSuccess(FirebaseModelOutputs result) {
                                                                            CUSTOM_RESULTS = result;
                                                                            analysisFinished();
                                                                        }
                                                                    })
                                                            .addOnFailureListener(
                                                                    new OnFailureListener() {
                                                                        @Override
                                                                        public void onFailure(@NonNull Exception e) {
                                                                            loadingBox.dismiss();
                                                                            loadingBox = ProgressDialog.show(AnalysisWait.this,
                                                                                    "Analysis Failed",
                                                                                    "Image analysis failed, returning to home...",
                                                                                    true);
                                                                            // delay of 1.5 seconds post failure
                                                                            timerDelayRemoveDialog(1500, loadingBox);
                                                                            finish();
                                                                        }
                                                                    });
                                                } catch (FirebaseMLException e) {
                                                    e.printStackTrace();
                                                    loadingBox.dismiss();
                                                    loadingBox = ProgressDialog.show(AnalysisWait.this,
                                                            "Failure Encountered",
                                                            "Custom interpreter failed, returning to home...",
                                                            true);
                                                    // delay of 1.5 seconds post failure
                                                    timerDelayRemoveDialog(1500, loadingBox);
                                                }
                                            }
                                        });

                            }
                        })
                        .addOnFailureListener(
                                new OnFailureListener() {
                                    @Override
                                    public void onFailure(@NonNull Exception e) {
                                        loadingBox.dismiss();
                                        loadingBox = ProgressDialog.show(AnalysisWait.this,
                                                "Failure Encountered",
                                                "Text detection failed, returning to home...",
                                                true);
                                        // delay of 1.5 seconds post failure
                                        timerDelayRemoveDialog(1500, loadingBox);
                                    }
                                });
    }

    // method to pass intent and continue flow after models are done executing
    private void analysisFinished() {
        // OCR
    }

    // creates input array for ML model
    private int[][][][] getScaledImageValue() {
        int[][][][] input = null;
        try {
            Bitmap bitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), imageUri);
            bitmap = Bitmap.createScaledBitmap(bitmap, 224, 224, true);

            int batchNum = 0;
            input = new int[1][512][512][3];
            for (int x = 0; x < 512; x++) {
                for (int y = 0; y < 512; y++) {
                    int pixel = bitmap.getPixel(x, y);
                    // Normalize channel values to [-1.0, 1.0]. This requirement varies by
                    // model. For example, some models might require values to be normalized
                    // to the range [0.0, 1.0] instead.
                    input[batchNum][x][y][0] = (Color.red(pixel) - 127) / 128;
                    input[batchNum][x][y][1] = (Color.green(pixel) - 127) / 128;
                    input[batchNum][x][y][2] = (Color.blue(pixel) - 127) / 128;
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
            loadingBox.dismiss();
            loadingBox = ProgressDialog.show(AnalysisWait.this,
                    "Failure Encountered",
                    "Bitmap creation failed, returning to home...",
                    true);
            // delay of 1.5 seconds post failure
            timerDelayRemoveDialog(1500, loadingBox);
            finish();
            return null;
        }
        return input;
    }


    // delay timer for dialog boxes, then returns to home
    public void timerDelayRemoveDialog(long time, final Dialog d){
        Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            public void run() {
                d.dismiss();
            }
        }, time);
        finish();
    }

}
