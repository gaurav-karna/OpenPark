package com.example.openpark;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import java.util.ArrayList;


public class AnalysisFinished extends AppCompatActivity {

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

        // set text values for OCR
        TextView ocr = (TextView) findViewById(R.id.ocrText);
        ocr.setText(analysisData.getString("ocrResult"));


        // set values for custom
        // cleaning data
        String customResults = "";
        for (String aResult : analysisData.getStringArrayList("customResult")) {
            customResults = customResults + aResult;
        }
        TextView custom = (TextView) findViewById(R.id.customText);
        custom.setText(customResults);
    }

    public void returnHome (View view) {
        Intent returnToHome = new Intent(this, MainActivity.class);
        returnToHome.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(returnToHome);
    }

}
