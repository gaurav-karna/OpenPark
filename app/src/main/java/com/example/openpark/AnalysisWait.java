package com.example.openpark;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;

import com.example.openpark.MainActivity;

public class AnalysisWait extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_analysis_wait);

        Intent analysisIntent = getIntent();

        ImageView iv = (ImageView) findViewById(R.id.croppedImage);
        iv.setImageURI(Uri.parse(analysisIntent.getStringExtra("scaled_image")));
    }

    public void tryAgain(View view) {
//        MainActivity.getInstance().dispatchTakePictureIntent(view);
        finish();
    }

    public void analyzeImage(View view) {
        // take user to loading screen while algo runs...

    }
}
