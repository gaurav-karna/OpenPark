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

import com.google.firebase.ml.custom.FirebaseModelOutputs;
import com.google.firebase.ml.vision.FirebaseVision;
import com.google.firebase.ml.vision.common.FirebaseVisionImage;
import com.google.firebase.ml.vision.text.FirebaseVisionText;
import com.google.firebase.ml.vision.text.FirebaseVisionTextRecognizer;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class AnalysisWait extends AppCompatActivity {

    // image to analyze URI
    private static Uri imageUri;

    // Name of ML model currently in use
    private static final String CURRENT_MODEL_NAME = "openpark_final_v1";

    // path to current model
    private static final String MODEL_PATH = "model.tflite";

    // loading screen
    ProgressDialog loadingBox;

    // variables to hold OCR and custom results
    FirebaseVisionText OCR_RESULTS;
    ArrayList<String> CUSTOM_RESULTS_STRING;
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

                                try {
                                    OpenparkTFAnalysis customRunner = new OpenparkTFAnalysis();
                                    ArrayList<String> customResults = customRunner.analysisAPI(
                                            imageUri, AnalysisWait.this
                                    );
                                    CUSTOM_RESULTS_STRING = customResults;
                                    loadingBox.dismiss();
                                    analysisFinished();
                                } catch (Exception e) {
                                    e.printStackTrace();
                                    loadingBox.dismiss();
                                    loadingBox = ProgressDialog.show(AnalysisWait.this,
                                            "Failure Encountered",
                                            "Custom run failed, returning to home...",
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


        analysisData.putStringArrayList("customResult", CUSTOM_RESULTS_STRING);
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
