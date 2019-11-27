package com.example.openpark;

import androidx.fragment.app.FragmentActivity;

import android.content.Intent;
import android.location.Location;
import android.os.Bundle;
import android.os.Parcelable;

import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

import java.util.ArrayList;

public class ParkMap extends FragmentActivity implements OnMapReadyCallback {

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


    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        Intent intent = getIntent();
        // adding markers for each parking in the city
        for (Parcelable toAdd : intent.getParcelableArrayListExtra(MainActivity.GEOPOINTS)) {
            Location loc = (Location) toAdd;    // GEOPOINTS will only ever have Location objects
            LatLng loc_to_add = new LatLng(loc.getLatitude(), loc.getLongitude());
            mMap.addMarker(new MarkerOptions().position(loc_to_add));
        }

//        System.out.println("Here is the list of coordinates pulled from Firestore");
//        for (Location x: coords) {
//            System.out.println("Latitude: " + x.getLatitude() + " Longitude: " + x.getLongitude());
//        }
//        for (Location x: self_location) {
//            System.out.println("Self Location: " + x.getLatitude() + " " + x.getLongitude());
//        }
//        System.out.println("Done");

        // moving map to self_location (which will always be the first and only Location in SELF_LOC)
        Location self_loc = (Location) intent.getParcelableArrayListExtra(MainActivity.SELF_LOC).get(0);
        LatLng self_latlng = new LatLng(self_loc.getLatitude(), self_loc.getLongitude());
        mMap.addMarker(new MarkerOptions().position(self_latlng).title("Your Location"));
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(self_latlng, zoomLevel));
    }
}
