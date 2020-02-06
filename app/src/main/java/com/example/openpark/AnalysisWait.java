package com.example.openpark;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.ImageView;

public class AnalysisWait extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_analysis_wait);

        Intent analysisIntent = getIntent();

        ImageView iv = (ImageView) findViewById(R.id.croppedImage);
        iv.setImageURI(Uri.parse(analysisIntent.getStringExtra("scaled_image")));
    }
}
