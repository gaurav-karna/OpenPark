package com.example.openpark;

import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.FragmentActivity;

import android.content.DialogInterface;
import android.content.Intent;
import android.location.Location;
import android.os.Bundle;
import android.os.Parcelable;
import android.view.View;

import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import java.util.ArrayList;

public class ParkMap
        extends FragmentActivity
        implements OnMapReadyCallback ,
        GoogleMap.OnMarkerClickListener
{

    private GoogleMap mMap;
    private float zoomLevel = 17f;      // global default zoom level

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_park_map);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
    }


    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        Intent intent = getIntent();
        // adding markers for each parking in the city

        ArrayList<OpenParkFirestoreDocument> allSigns =
                (ArrayList<OpenParkFirestoreDocument>)
                        intent.getSerializableExtra(MainActivity.GEOPOINTS);

        for (OpenParkFirestoreDocument sign : allSigns) {
            // parse location into Location object to feed into GMaps
            // no provider since current location is not needed for query
            Location toAdd = new Location("");

            // according to format set in AnalysisWait
            String[] parkLocationCoords = sign.getLocation().split(",");

            // parse lat and long as doubles and add them into toAdd
            toAdd.setLatitude(Double.parseDouble(parkLocationCoords[0]));
            toAdd.setLongitude(Double.parseDouble(parkLocationCoords[1]));

            // make LatLng from Location object
            LatLng loc_to_add = new LatLng(toAdd.getLatitude(), toAdd.getLongitude());
            Marker currentMarker = mMap.addMarker(new MarkerOptions()
                    .position(loc_to_add).title("Parking Sign")
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE))
            );

            currentMarker.setTag(sign.mapInformation());    // associating this marker with its data

        }


        // moving map to self_location (will always be the first and only Location in SELF_LOC)
        Location self_loc = (Location) intent.getParcelableArrayListExtra(
                                              MainActivity.SELF_LOC).get(0);
        LatLng self_latlng = new LatLng(self_loc.getLatitude(), self_loc.getLongitude());

        mMap.addMarker(new MarkerOptions().position(self_latlng).title("Your Location"));
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(self_latlng, zoomLevel));

        // tell map to listen to marker clicks
        mMap.setOnMarkerClickListener(this);
    }


    @Override
    public boolean onMarkerClick(final Marker marker) {
        // retrieve data stored with getTag(), and display as Alert Dialog
        try {
            AlertDialog alertDialog = new AlertDialog.Builder(
                    ParkMap.this).create();
            alertDialog.setTitle(marker.getTitle() + " Information");
            alertDialog.setMessage((String) marker.getTag());
            alertDialog.setButton(AlertDialog.BUTTON_NEUTRAL, "OK",
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss();
                        }
                    });
            alertDialog.show();
            return true;        // operation successful
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;   // in case information could not be retrieved
    }

}
