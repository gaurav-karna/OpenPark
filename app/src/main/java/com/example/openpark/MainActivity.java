package com.example.openpark;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;

import android.app.ProgressDialog;
import android.content.ActivityNotFoundException;
import android.content.DialogInterface;
import android.content.Intent;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.provider.MediaStore;
import android.view.View;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.GeoPoint;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;



public class MainActivity extends AppCompatActivity {

    // will consist of array of Firestore geopoint types queried from DB
    public static final String GEOPOINTS = "com.example.OpenPark.GEOPOINTS";

    // self location passed into map
    public static final String SELF_LOC = "com.exmaple.OpenPark.SELF_LOC";

    // save instance of class
    private static MainActivity instance;

    // used to get location of user, and define a radius from which we will get geopoints
    private FusedLocationProviderClient fusedLocationClient;

    // user's city location, used in querying the DB (HARD SET TO 'montreal' for now)
    private String currentCity = "montreal";

    // Firestore client used to query
    private FirebaseFirestore DB_CLIENT = FirebaseFirestore.getInstance();

    // list of coordinates for parkings in a city
    public ArrayList<Location> coords = new ArrayList<>();  // using Location b/c it's serializable

    // initing self location
    public ArrayList<Location> self_location = new ArrayList<>(); // will only be 1 unit long

    // some variables we will need for the take picture and crop mechanism
    static final int REQUEST_TAKE_PHOTO = 1;
    static final int CROP_CODE = 2;
    private Uri picUri;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // setting instance
        instance = this;

        /* TODO: correctly init currentCity
        When implementing the Users model in the future, it would be useful to have a private
        variable dedicated to storing the user's city. Currently, it is hard set to 'montreal'
         */
        // init the Location client, and get fine location of user
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        /* TODO: Currently no way to query Firestore geopoints to get the closest parking spots...
           Circumvented by loading all parking spots in the collection of the user (city,
           currently hard set to 'montreal'), but zooming in close to the user's location.
         */

        fusedLocationClient.getLastLocation()
                .addOnSuccessListener(this, new OnSuccessListener<Location>() {
                    @Override
                    public void onSuccess(Location location) {
                        // Got last known location. In some rare situations this can be null.
                        if (location != null) {
                            // initing coords
                            queryDB();

                            // setting up self_location
                            self_location.clear();
                            self_location.add(location);
                        } else { // let user know to try again in a few minutes
                            AlertDialog alertDialog = new AlertDialog.Builder(
                                    MainActivity.this).create();
                            alertDialog.setTitle("Could not get location");
                            alertDialog.setMessage("We had trouble fetching your location. " +
                                    "Try again in a few minutes!");
                            alertDialog.setButton(AlertDialog.BUTTON_NEUTRAL, "OK",
                                    new DialogInterface.OnClickListener() {
                                        public void onClick(DialogInterface dialog, int which) {
                                            dialog.dismiss();
                                        }
                                    });
                            alertDialog.show();
                        }
                    }
                });
    }

    public static MainActivity getInstance() {
        return instance;
    }

    // Starts the ParkMap activity, opens the Map API, populating it with info from DB
    public void triggerMap (View view) {
        Intent trigger = new Intent(this, ParkMap.class);


        // have a loading screen on main thread for 3 seconds,
        // while GLOBAL coords and self_location populate
        final ProgressDialog progress = new ProgressDialog(MainActivity.this);
        progress.setTitle("Loading...");
        progress.setMessage("Please wait while we fetch data from our servers...");
        progress.show();

        Runnable progressRunnable = new Runnable() {

            @Override
            public void run() {
                progress.cancel();
            }

        };

        Handler pdCanceller = new Handler();
        pdCanceller.postDelayed(progressRunnable, 3000);

        // adding locations of parkings to Intent
        trigger.putParcelableArrayListExtra(GEOPOINTS, coords);
        trigger.putParcelableArrayListExtra(SELF_LOC, self_location);

        startActivity(trigger);
    }

    public void pickScan (View view) {
        final String[] options = {"Camera", "Photo Library"};

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("How would you like to scan the sign?");
        builder.setItems(options, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int picked) {
                // the user clicked on colors[which]
                if (options[picked].equals("Camera")) {
                    dispatchTakePictureIntent(null);
                } else if (options[picked].equals(("Photo Library"))) {

                }
            }
        });
        builder.show();
    }

    // Starts the scanning mechanism - Camera intent & Cropping
    static final int REQUEST_IMAGE_CAPTURE = 1;
    String currentPhotoPath;

    // helper method to create collision-resistant timestamps
    private File createImageFile() throws IOException {
        // Create an image file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        File image = File.createTempFile(
                imageFileName,  /* prefix */
                ".jpg",         /* suffix */
                storageDir      /* directory */
        );

        // Save a file: path for use with ACTION_VIEW intents
        currentPhotoPath = image.getAbsolutePath();
        return image;
    }

    // STARTS camera app and saves photo
    public void dispatchTakePictureIntent(View view) {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        // Ensure that there's a camera activity to handle the intent
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            // Create the File where the photo should go
            File photoFile = null;
            try {
                photoFile = createImageFile();
            } catch (IOException ex) {
                // Error occurred while creating the File
                return;
            }
            // Continue only if the File was successfully created
            if (photoFile != null) {
                Uri photoURI = FileProvider.getUriForFile(this,
                        "com.example.android.fileprovider",
                        photoFile);
                picUri = photoURI;
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
                startActivityForResult(takePictureIntent, REQUEST_TAKE_PHOTO);
            }
        }
    }

    // function to handle image data once intent is fulfilled -- cropping mechanism
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
//        System.out.println("Picture taken");
        if (resultCode == RESULT_OK) {
            if (requestCode == REQUEST_IMAGE_CAPTURE) {
                // Uri of pic saved in global var 'picUri'
                cropPicture();
            } else if (requestCode == CROP_CODE) {
                // delegate to analyze the cropped image
                Intent analysis = new Intent(this, AnalysisWait.class);
                analysis.putExtra("scaled_image", picUri.toString()); // add image to intent
                startActivity(analysis);
            }
        }
    }

    /*
    TODO: MAJOR: make Parking class, and instantiate that rather than getting geopoint from query
    - this will allow us to display information about the parking upon clicking the pin in question
     */

    // simply queries the DB with currentCity attribute, and returns LatLng of all signs in city
    public void queryDB() {
        coords.clear();
        this.DB_CLIENT.collection(currentCity)
                .get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        if (task.isSuccessful()) {

                            for (QueryDocumentSnapshot document : task.getResult()) {
                                GeoPoint gp = document.getGeoPoint("location");
                                Location toAdd = new Location("");
                                toAdd.setLatitude(gp.getLatitude());
                                toAdd.setLongitude(gp.getLongitude());
                                coords.add(toAdd);
                            }

                        } else {
                            AlertDialog alertDialog = new AlertDialog.Builder(
                                    MainActivity.this).create();
                            alertDialog.setTitle("Could not get information");
                            alertDialog.setMessage(
                            "We had trouble getting some information. Check your network " +
                                    "connectivity, or try again in a few minutes!");
                            alertDialog.setButton(AlertDialog.BUTTON_NEUTRAL, "OK",
                                    new DialogInterface.OnClickListener() {
                                        public void onClick(DialogInterface dialog, int which) {
                                            dialog.dismiss();
                                        }
                                    });
                            alertDialog.show();
                        }
                    }
                });
    }

    // private method to perform the cropping of an image captured by the camera
    private void cropPicture() {
        try {
            Intent cropIntent = new Intent("com.android.camera.action.CROP");
            //indicate image type and Uri
            cropIntent.setDataAndType(picUri, "image/*");

            //set crop properties
            cropIntent.putExtra("crop", "true");

            //retrieve data on return
            cropIntent.putExtra("return-data", true);
            cropIntent.putExtra(MediaStore.EXTRA_OUTPUT, picUri);

            // adding permission so camera can write cropped image to provided Uri
            cropIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            cropIntent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION);

            //start the activity - we handle returning in onActivityResult
            startActivityForResult(cropIntent, CROP_CODE);
        }
        catch(ActivityNotFoundException anfe){
            //display an error message
            AlertDialog alertDialog = new AlertDialog.Builder(MainActivity.this).create();
            alertDialog.setTitle("Unable to crop");
            alertDialog.setMessage(
                    "Your device is unable to crop this image.");
            alertDialog.setButton(AlertDialog.BUTTON_NEUTRAL, "OK",
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss();
                        }
                    });
            alertDialog.show();
        }
    }
}
