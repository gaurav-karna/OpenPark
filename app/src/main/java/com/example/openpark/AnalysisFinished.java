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

//        // construct label list as text
//        String labelList = "";
//        ArrayList<String> labelResults = analysisData.getStringArrayList("customResult");
//        for (String label : labelResults) {
//            labelList = labelList + label + "\n";
//        }
//
//        // set values for custom
//        TextView custom = (TextView) findViewById(R.id.customText);
//        custom.setText(labelList);

        // construct probability list as text
        String probabilityList = "";
        ArrayList<float[]> customResultList = (ArrayList<float[]>) analysisData.getSerializable("customResult");
        // customResultList is a list of all the subarrays of results (max size 40)

        for (int i = 0; i < customResultList.size(); i++) {
            if (i < customLabels.length) {
                // concatenate with a label
                probabilityList = probabilityList + customLabels[i] + ": ";
                for (float stat : customResultList.get(i)) {
                    probabilityList = probabilityList + stat + ", ";
                }
                // concatenate with a newline
                probabilityList = probabilityList + "\n";
            } else {
                // concatenate with the counter
                probabilityList = probabilityList + (i+1) + ": ";
                for (float stat : customResultList.get(i)) {
                    probabilityList = probabilityList + stat + ", ";
                }
                // concatenate with a newline
                probabilityList = probabilityList + "\n";
            }

        }

        // set values for custom
        TextView custom = (TextView) findViewById(R.id.customText);
        custom.setText(probabilityList);
    }

    public void returnHome (View view) {
        Intent returnToHome = new Intent(this, MainActivity.class);
        returnToHome.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(returnToHome);
    }

}
