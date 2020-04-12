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
import java.util.HashMap;
import java.util.List;

public class AnalysisWait extends AppCompatActivity {

    // Global Vars for fuzzy search
    public boolean NO_PARKING_FOUND = false;
    public boolean NO_STOPPING_FOUND = false;
    public boolean PARKING_FOUND = false;
    public boolean PARKING_EXCEPTION_FOUND = false;
    public boolean SECTORS_FOUND = false;

    // time to initialize parameter booleans
    boolean HOURS_RANGE_TEXTPARAM = false;
    boolean DAYS_OF_WEEK_TEXTPARAM = false;
    boolean TIME_OF_YEAR_TEXTPARAM = false;
    boolean TIME_LIMIT_TEXTPARAM = false;
    boolean SECTORS_TEXTPARAM = false;

    // since multiple sectors could be on a sign, we append to this string and then add to
    // displayMapper if not null
    String SECTOR_DETECTED_SIGNS = null;

    // global FuzzySearch initializer
    OpenParkFuzzySearch lineProcesser;

    // global Map of Strings to display and whether they were found or not (null if not)
    HashMap<String, String> displayMapper;

    // custom labels defined in arrayList below
    private String[] customLabels = {
            "no_parking",
            "no_stopping",
            "parking",
            "parking_exception",
            "sectors"
    };

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

    // to pass into intent
    String CUSTOM_RESULTS_PROCESSED;

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
        loadingBox = ProgressDialog.show(this, "Processing 1/3",
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
                                        "Processing 2/3", "Analyzing image...",
                                        true);

                                try {
                                    OpenparkTFAnalysis customRunner = new OpenparkTFAnalysis();
                                    ArrayList<String> customResults = customRunner.analysisAPI(
                                            imageUri, AnalysisWait.this
                                    );
                                    CUSTOM_RESULTS_STRING = customResults;
                                    loadingBox.dismiss();

                                    // update loading box (Stage 3, OCR Fuzzy search)
                                    loadingBox = ProgressDialog.show(AnalysisWait.this,
                                            "Processing 3/3", "Parsing text...",
                                            true);
                                    // begin fuzzy search
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

        // string arrayList has all labels over 50% confidence
        for (String aResult : CUSTOM_RESULTS_STRING) {
            // check which labels exist in the arrayList by cross-referencing all possible labels
            if (aResult.contains(customLabels[0])) {
                NO_PARKING_FOUND = true;
            }
            if (aResult.contains(customLabels[1])) {
                NO_STOPPING_FOUND = true;
            }
            if (aResult.contains(customLabels[2])) {
                PARKING_FOUND = true;
            }
            if (aResult.contains(customLabels[3])) {
                PARKING_EXCEPTION_FOUND = true;
            }
            if (aResult.contains(customLabels[4])) {
                SECTORS_FOUND = true;
            }
        }

        // global booleans have been initialized, it's now time to do the fuzzy search
        lineProcesser = new OpenParkFuzzySearch();

        // initialize the displayMapper
        initializeDisplayMapper();

        // convert all Blocks into a List of Lines; used Lines will be removed from the list
        ArrayList<FirebaseVisionText.Line> ocrLines = new ArrayList<>();
        for (FirebaseVisionText.TextBlock currentBlock : OCR_RESULTS.getTextBlocks()) {
            for (FirebaseVisionText.Line lineToAdd : currentBlock.getLines()) {
                ocrLines.add(lineToAdd);
            }
        }

        // holder for all unused lines
        ArrayList<FirebaseVisionText.Line> leftoverOCRLines = new ArrayList<>();

        // iterate through all lines and feed into processLine method
        for (FirebaseVisionText.Line currentLine : ocrLines) {
            if (!(processLine(currentLine))) {
                leftoverOCRLines.add(currentLine);
            }
        }

        // at this point, unused Lines are in leftoverOCRLines
        // constructing extra Information string
        if (!(leftoverOCRLines.isEmpty())) {
            String extraInfo = "";
            for (FirebaseVisionText.Line currentLine : leftoverOCRLines) {
                extraInfo += currentLine.getText() + "\n";
            }
            // adding extraInfo into displayMapper
            displayMapper.put("extraInformation", extraInfo);
        }

        // TODO: future functionality, extract location from image

        // construct output strings with non-null values in displayMapper
        CUSTOM_RESULTS_PROCESSED = constructDisplayString();

        // OCR and custom results stored in global variables
        Intent viewAnalysisResults = new Intent(this, AnalysisFinished.class);
        // bundle the analysis Data
        Bundle analysisData = new Bundle();
        analysisData.putString("ocrResult", CUSTOM_RESULTS_PROCESSED);

        // Blocking out percentages for right now
//        analysisData.putStringArrayList("customResult", CUSTOM_RESULTS_STRING);
        viewAnalysisResults.putExtras(analysisData);

        // close 3rd loading box before launching next activity
        loadingBox.dismiss();
        startActivity(viewAnalysisResults);
    }

    // process method for Lines
    private boolean processLine(FirebaseVisionText.Line line) {
        // cycle through methods unless marked as found

        // search for time range
        if (!HOURS_RANGE_TEXTPARAM) {
            HOURS_RANGE_TEXTPARAM = lineProcesser.searchForTimeRange(line);
            if (HOURS_RANGE_TEXTPARAM) {
                displayMapper.put("timeOfDay", line.getText());
                return true;
            }
        }

        // search for days of week
        if (!DAYS_OF_WEEK_TEXTPARAM) {
            DAYS_OF_WEEK_TEXTPARAM = lineProcesser.searchForDaysOfWeek(line);
            if (DAYS_OF_WEEK_TEXTPARAM) {
                displayMapper.put("daysOfWeek", line.getText());
                return true;
            }
        }

        // search for months of year
        if (!TIME_OF_YEAR_TEXTPARAM) {
            TIME_OF_YEAR_TEXTPARAM = lineProcesser.searchForTimeOfYear(line);
            if (TIME_OF_YEAR_TEXTPARAM) {
                displayMapper.put("timeOfYear", line.getText());
                return true;
            }
        }

        // search for time limit
        if (!TIME_LIMIT_TEXTPARAM) {
            TIME_LIMIT_TEXTPARAM = lineProcesser.searchForTimeAllowed(line);
            if (TIME_LIMIT_TEXTPARAM) {
                displayMapper.put("timeLimit", line.getText());
                return true;
            }
        }

        // search for sector if PARKING_EXCEPTION and SECTORS are true
        if ((SECTORS_FOUND && PARKING_EXCEPTION_FOUND)) {
            // No check for SECTORS_TEXTPARAM because there could be multiple sectors
            SECTORS_TEXTPARAM = lineProcesser.searchForSector(line);
            if (SECTORS_TEXTPARAM) {
                // multiple sectors - append to Global String then set "sector" to global var if !null
                // first time detected, set to not null and append
                if (SECTOR_DETECTED_SIGNS == null) {
                    SECTOR_DETECTED_SIGNS = line.getText();
                } else {    // not null, so just append
                    SECTOR_DETECTED_SIGNS += ", " + line.getText();
                }
                return true;
            }
        }

        return false;
    }

    public void initializeDisplayMapper() {
        displayMapper = new HashMap<>();
        // can park
        if (NO_PARKING_FOUND) {
            displayMapper.put("canPark", "no");
        } else if (PARKING_FOUND) {
            displayMapper.put("canPark", "yes");
        } else if (NO_STOPPING_FOUND) {
            displayMapper.put("canPark", "stop");
        }
        // time of year
        displayMapper.put("timeOfYear", null);

        // days of week
        displayMapper.put("daysOfWeek", null);

        // time of days
        displayMapper.put("timeOfDay", null);

        // time limit
        displayMapper.put("timeLimit", null);

        // sector
        displayMapper.put("sector", null);

        // extra information
        displayMapper.put("extraInformation", null);

        // location
        displayMapper.put("location", null);
    }

    // builds the String to pass into the ActivityFinished class
    private String constructDisplayString() {
        String display = "Park here?\n";

        if (displayMapper.get("canPark") != null) {
            if (displayMapper.get("canPark").equals("yes")) {
                display += "- You can park here under conditions:\n";
            } else if (displayMapper.get("canPark").equals("no")) {
                display += "- You can't park here under conditions:\n";
            } else if (displayMapper.get("canPark").equals("stop")) {
                display += "- You can't stop here under conditions:\n";
            }
        } else {
            display += "- None found\n";
        }

        // conditions
        display += "\nConditions:\n";
        boolean conditionsFound = false;       // one-time flag, otherwise 'None found'

        // cycle through all conditions
        if (displayMapper.get("timeOfYear") != null) {
            conditionsFound = true;
            display += "- During this time of year: " + displayMapper.get("timeOfYear") + "\n";
        }

        if (displayMapper.get("daysOfWeek") != null) {
            conditionsFound = true;
            display += "- On the day(s): " + displayMapper.get("daysOfWeek") + "\n";
        }

        if (displayMapper.get("timeOfDay") != null) {
            conditionsFound = true;
            display += "- In this time range of day: " + displayMapper.get("timeOfDay") + "\n";
        }

        if (displayMapper.get("timeLimit") != null) {
            conditionsFound = true;
            display += "- For this amount of time: " + displayMapper.get("timeLimit") + "\n";
        }

        if (SECTOR_DETECTED_SIGNS != null) {        // means at least one sector was found
            displayMapper.put("sector", SECTOR_DETECTED_SIGNS);     // add all found sectors to displayMapper
            conditionsFound = true;
            display += "- Above don't apply if vehicle has permit(s): " +
                    displayMapper.get("sector") + "\n";
        }

        if (!(conditionsFound)) {
            display += "None found";
        }

        if (displayMapper.get("extraInformation") != null) {
            display += "\nExtra Information found:\n" + displayMapper.get("extraInformation");
        }

        if (displayMapper.get("location") != null) {
            display += "\nLocation: " + displayMapper.get("location");
        }

        return display;
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
