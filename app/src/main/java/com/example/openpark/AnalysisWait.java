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
import com.google.firebase.ml.vision.automl.FirebaseAutoMLLocalModel;
import com.google.firebase.ml.vision.common.FirebaseVisionImage;
import com.google.firebase.ml.vision.label.FirebaseVisionImageLabel;
import com.google.firebase.ml.vision.label.FirebaseVisionImageLabeler;
import com.google.firebase.ml.vision.label.FirebaseVisionOnDeviceAutoMLImageLabelerOptions;
import com.google.firebase.ml.vision.text.FirebaseVisionText;
import com.google.firebase.ml.vision.text.FirebaseVisionTextRecognizer;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class AnalysisWait extends AppCompatActivity {

    // image to analyze URI
    private static Uri imageUri;

    // Name of ML model currently in use
    private static final String CURRENT_MODEL_NAME = "openpark_final_v1";

    // path to current model
    private static final String MODEL_PATH = "assets/model.tflite";

    // loading screen
    ProgressDialog loadingBox;

    // variables to hold OCR and custom results
    FirebaseVisionText OCR_RESULTS;
    ArrayList<String> CUSTOM_RESULTS;

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
                                final FirebaseAutoMLLocalModel localModel = new FirebaseAutoMLLocalModel.Builder()
                                        .setAssetFilePath("tflite_metadata.json")
                                        .build();

                                FirebaseVisionImageLabeler labeler;
                                try {
                                    FirebaseVisionOnDeviceAutoMLImageLabelerOptions options =
                                            new FirebaseVisionOnDeviceAutoMLImageLabelerOptions.Builder(localModel)
                                                    .setConfidenceThreshold(0.7f)  // Evaluate your model in the Firebase console
                                                    // to determine an appropriate value.
                                                    .build();
                                    labeler = FirebaseVision.getInstance().getOnDeviceAutoMLImageLabeler(options);

                                    FirebaseVisionImage image;

                                    image = FirebaseVisionImage.fromFilePath(AnalysisWait.this, imageUri);

                                    labeler.processImage(image)
                                            .addOnSuccessListener(new OnSuccessListener<List<FirebaseVisionImageLabel>>() {
                                                @Override
                                                public void onSuccess(List<FirebaseVisionImageLabel> labels) {
                                                    // Task completed successfully
                                                    loadingBox.dismiss();
                                                    ArrayList<String> labelResults = new ArrayList<>();
                                                    for (FirebaseVisionImageLabel label: labels) {
                                                        labelResults.add(label.getText() + " - " + label.getConfidence());
                                                    }
                                                    CUSTOM_RESULTS = labelResults;
                                                    analysisFinished();
                                                }
                                            })
                                            .addOnFailureListener(new OnFailureListener() {
                                                @Override
                                                public void onFailure(@NonNull Exception e) {
                                                    loadingBox.dismiss();
                                                    loadingBox = ProgressDialog.show(AnalysisWait.this,
                                                            "Image Analysis Failed",
                                                            "Custom analysis failed, returning to home...",
                                                            true);
                                                    // delay of 1.5 seconds post failure
                                                    timerDelayRemoveDialog(1500, loadingBox);
                                                }
                                            });

                                } catch (FirebaseMLException e) {
                                    loadingBox.dismiss();
                                    loadingBox = ProgressDialog.show(AnalysisWait.this,
                                            "Failure Encountered",
                                            "On device labeller failed, returning to home...",
                                            true);
                                    // delay of 1.5 seconds post failure
                                    timerDelayRemoveDialog(1500, loadingBox);
                                } catch (IOException e) {
                                    e.printStackTrace();
                                    loadingBox.dismiss();
                                    loadingBox = ProgressDialog.show(AnalysisWait.this,
                                            "FirebaseImage Uri Failure Encountered",
                                            "IO Exception with Firebase Uri, returning to home...",
                                            true);
                                    // delay of 1.5 seconds post failure
                                    timerDelayRemoveDialog(1500, loadingBox);
                                }

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
        // OCR and custom results stored in global variables
        Intent viewAnalysisResults = new Intent(this, AnalysisFinished.class);
        // bundle the analysis Data
        Bundle analysisData = new Bundle();
        analysisData.putString("ocrResult", OCR_RESULTS.getText());

        analysisData.putStringArrayList("customResult", CUSTOM_RESULTS);
        viewAnalysisResults.putExtras(analysisData);
        startActivity(viewAnalysisResults);
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
