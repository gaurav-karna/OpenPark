package com.example.openpark;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.os.Parcelable;
import android.os.SystemClock;
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
import com.google.android.gms.maps.model.LatLng;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;


public class MainActivity extends AppCompatActivity {

    // will consist of array of Firestore geopoint types queried from DB
    public static final String GEOPOINTS = "com.example.OpenPark.GEOPOINTS";

    // self location passed into map
    public static final String SELF_LOC = "com.exmaple.OpenPark.SELF_LOC";

    // used to get location of user, and define a radius from which we will get geopoints
    private FusedLocationProviderClient fusedLocationClient;

    // user's city location, used in querying the DB (HARD SET TO 'montreal' for now)
    private String currentCity = "montreal";

    // Firestore client used to query
    private FirebaseFirestore DB_CLIENT = FirebaseFirestore.getInstance();

    // list of coordinates for parkings in a city
    public ArrayList<Location> coords = new ArrayList<>();  // using Location because it's serializable

    // initing self location
    public ArrayList<Location> self_location = new ArrayList<>(); // in reality, will only be 1 unit long

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
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
                            AlertDialog alertDialog = new AlertDialog.Builder(MainActivity.this)
                                    .create();
                            alertDialog.setTitle("Could not get location");
                            alertDialog.setMessage("We had trouble fetching your location. Try again in a few minutes!");
                            alertDialog.setButton(AlertDialog.BUTTON_NEUTRAL, "OK",
                                    new DialogInterface.OnClickListener() {
                                        public void onClick(DialogInterface dialog, int which) {
                                            dialog.dismiss();
                                        }
                                    });
                            alertDialog.show();
                            return;
                        }
                    }
                });
    }

    // Starts the ParkMap activity, opens the Map API, populating it with info from DB
    public void triggerMap (View view) {
        Intent trigger = new Intent(this, ParkMap.class);

//         Debug block - START
//        System.out.println("Here is the list of coordinates pulled from Firestore");
//        for (Location x: coords) {
//            System.out.println("Latitude: " + x.getLatitude() + " Longitude: " + x.getLongitude());
//        }
//        for (Location x: self_location) {
//            System.out.println("Self Location: " + x.getLatitude() + " " + x.getLongitude());
//        }
//        System.out.println("Done");
//         Debug block - END


        // have a loading screen on main thread for 3 seconds, while GLOBAL coords and self_location populate
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

    /*
    TODO: MAJOR: make Parking class, and instantiate that instead of just getting geopoint from query
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
                            AlertDialog alertDialog = new AlertDialog.Builder(MainActivity.this).create();
                            alertDialog.setTitle("Could not get information");
                            alertDialog.setMessage(
                            "We had trouble getting some information. Check your network connectivity, or try again in a few minutes!");
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
}
