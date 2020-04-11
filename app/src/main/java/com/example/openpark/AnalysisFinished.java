package com.example.openpark;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import java.util.ArrayList;


public class AnalysisFinished extends AppCompatActivity {

    private String OCR_RESULTS;     // will hold the text results for sanitization
    private String OCR_RESULTS_DISPLAY;     // what the user will see after sanitization

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

        // set values for custom
        // cleaning data

        // set custom results as text for now
        TextView custom = (TextView) findViewById(R.id.customText);


        TextView ocr = (TextView) findViewById(R.id.ocrText);   // set text values for OCR
        OCR_RESULTS = analysisData.getString("ocrResult"); // store OCR Results

        // perform sanitization with initialized boolean labels
        makeSenseOfOCR();

        // set text results in OCR
        ocr.setText(analysisData.getString(OCR_RESULTS_DISPLAY));

    }

    private static void makeSenseOfOCR () {
        // use OCR_RESULTS and Global Booleans to find what relevant information OCR returned
    }

    public void returnHome (View view) {
        Intent returnToHome = new Intent(this, MainActivity.class);
        returnToHome.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(returnToHome);
    }

}
