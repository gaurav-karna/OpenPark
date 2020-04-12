package com.example.openpark;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.TextView;
import java.util.Calendar;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.HashMap;


public class AnalysisFinished extends AppCompatActivity {

    private String OCR_RESULTS_DISPLAY;     // what the user will see after sanitization
    private HashMap<String, String> firestoreMapper;    // displayMapper from AnalysisWait

    private FirebaseFirestore db;       // Firestore instance

    ProgressDialog loadingBox;          // loading screen

    // custom labels defined in arrayList below
    String[] customLabels = {
            "no_parking",
            "no_stopping",
            "parking",
            "parking_exception",
            "sectors"
    };

    // custom and ocr results transferred via intent

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_analysis_finished);

        // retrieve intent data
        Bundle analysisData = getIntent().getExtras();
        OCR_RESULTS_DISPLAY = analysisData.getString("ocrResult"); // OCR Results for display

        // below is getting the mapping for firestore documents
        HashMap<String, String> mapHolder = (HashMap<String, String>) analysisData.getSerializable("displayMapper");

        // deep copying into firestoreMapper
        firestoreMapper = new HashMap<String, String>();
        for (String key : mapHolder.keySet()) {
            firestoreMapper.put(key, mapHolder.get(key));
        }

        // setting text results in OCR
        TextView ocr = (TextView) findViewById(R.id.ocrText);
        ocr.setText(OCR_RESULTS_DISPLAY);

    }


    public void returnHome (View view) {
        Intent returnToHome = new Intent(this, MainActivity.class);
        returnToHome.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(returnToHome);
    }

    // will feed data into firestore
    public void feedToFirestore (View view) {
        // setting loading screen
        loadingBox = ProgressDialog.show(this, "Uploading Data",
                "Please wait...", true);

        // init firestore instance
        db = FirebaseFirestore.getInstance();

        // getting rid of any null values in mapper
        for (String key : firestoreMapper.keySet()) {
            if (firestoreMapper.get(key) == null) {
                firestoreMapper.put(key, "None");
            }
        }

        // constructing document to add
        OpenParkFirestoreDocument toAdd = new OpenParkFirestoreDocument(
                firestoreMapper.get("canPark"),
                Calendar.getInstance().getTime().toString(),
                firestoreMapper.get("daysOfWeek"),
                firestoreMapper.get("location"),
                firestoreMapper.get("sector"),
                firestoreMapper.get("timeLimit"),
                firestoreMapper.get("timeOfDay"),
                firestoreMapper.get("timeOfYear"),
                firestoreMapper.get("extraInformation")
        );

        db.collection(MainActivity.currentCity)
                .add(toAdd)
                .addOnSuccessListener(new OnSuccessListener<DocumentReference>() {
                    @Override
                    public void onSuccess(DocumentReference documentReference) {
                        loadingBox.dismiss();
                        // display success message
                        loadingBox = ProgressDialog.show(AnalysisFinished.this, "Upload Complete!",
                                "Returning to home...", true);
                        Intent returnToHome = new Intent(AnalysisFinished.this, MainActivity.class);
                        returnToHome.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                        loadingBox.dismiss();
                        startActivity(returnToHome);

                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        loadingBox.dismiss();
                        loadingBox = ProgressDialog.show(AnalysisFinished.this, "Upload Failed!",
                                "Error in upload - Returning to home...", true);
                        Intent returnToHome = new Intent(AnalysisFinished.this, MainActivity.class);
                        returnToHome.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                        // delay of 1.5 seconds post failure
                        timerDelayRemoveDialog(1500, loadingBox);
                        startActivity(returnToHome);
                    }
                });


    }

    // delay timer for dialog boxes, then returns to home
    public void timerDelayRemoveDialog(long time, final Dialog d){
        Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            public void run() {
                d.dismiss();
            }
        }, time);
    }

}
