package com.example.openpark;

import android.content.Context;
import android.graphics.Bitmap;
import android.net.Uri;
import android.provider.MediaStore;

import org.tensorflow.lite.Interpreter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class OpenparkTFAnalysis implements Classifier {
    // Configuration values for the prepackaged SSD model.
    private static final int TF_OD_API_INPUT_SIZE = 512;
    private static final boolean TF_OD_API_IS_QUANTIZED = true;
    private static final String TF_OD_API_MODEL_FILE = "model.tflite";
    private static final String TF_OD_API_LABELS_FILE = "file:///android_asset/dict.txt";
    private static int CONFIDENCE_FILTER = 50;  // % to include results sent back to AnalysisWait

    // just so we can implement Classifier
    private Interpreter tfLite;

    private Classifier detector;

    //this is the method we call to return the results back to AnalysisWait as Strings.
    public ArrayList<String> analysisAPI(Uri imageUri, Context c) throws Exception  {
        // create scaled Bitmap
        Bitmap bitmap = MediaStore.Images.Media.getBitmap(
                c.getContentResolver(),
                imageUri
        );

        bitmap = Bitmap.createScaledBitmap(bitmap, 512, 512, true);

        // creating detector
        try {
            detector =
                    TFAnalysis.create(
                            c.getAssets(),
                            TF_OD_API_MODEL_FILE,
                            TF_OD_API_LABELS_FILE,
                            TF_OD_API_INPUT_SIZE,
                            TF_OD_API_IS_QUANTIZED
                    );
        } catch (final IOException e) {
            e.printStackTrace();
            catchError("Classifier could not be initialized");
        }

        // running predictions
        final List<Classifier.Recognition> results = detector.recognizeImage(bitmap);

        // parsing results into String to return to Activity
        ArrayList<String> customResults = new ArrayList<>();
        for (Recognition validResult : results) {
            // only include results with confidence more than CONFIDENCE_FILTER
            if (((int) (validResult.getConfidence()*100)) >= CONFIDENCE_FILTER) {
                customResults.add(
                        validResult.getTitle() + ": " + validResult.getConfidence() + "\n"
                );
            }
        }

        if (customResults.isEmpty()) {
            catchError("No valid results achieved");
        }
        return customResults;
    }

    // if error
    private void catchError(String msg) throws Exception {
        Exception OpenparkTFAnalysisException = new Exception(msg);
        throw OpenparkTFAnalysisException;
    }


    @Override
    public List<Recognition> recognizeImage(final Bitmap bitmap) { return null; }

    @Override
    public void enableStatLogging(final boolean logStats) {}

    @Override
    public String getStatString() {
        return "";
    }

    @Override
    public void close() {}

    public void setNumThreads(int num_threads) {
        if (tfLite != null) tfLite.setNumThreads(num_threads);
    }

    @Override
    public void setUseNNAPI(boolean isChecked) {
        if (tfLite != null) tfLite.setUseNNAPI(isChecked);
    }
}
