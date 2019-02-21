package com.recharge;

import android.Manifest;
import android.content.pm.PackageManager;
import android.support.design.widget.BottomNavigationView;
import android.support.v4.app.ActivityCompat;
import android.content.Intent;
import android.net.Uri;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;

import android.support.v4.content.ContextCompat;
import android.view.MenuItem;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import org.json.JSONArray;
import org.json.JSONObject;


public class MapsActivity extends FragmentActivity implements OnMapReadyCallback {

    // Default location and zoom for map
    // Right now, this location is the Purdue University Engineering Fountain
    private final double DEFAULT_LOCLAT = 40.4286;
    private final double DEFAULT_LOCLNG = -86.9138;
    private final int DEFAULT_ZOOM = 15;

    private GoogleMap mMap;
    private final int REQUEST_ACCESS_FINE_LOCATION = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);

        // Set the listener for the navigation bar at the bottom of the screen
        BottomNavigationView navigation = findViewById(R.id.navigationView);
        navigation.setOnNavigationItemSelectedListener(mOnNavigationItemSelectedListener);

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
    }

    // This is the listener for the navigation bar at the bottom of the screen
    // This determines what should happen when each button is selected
    private BottomNavigationView.OnNavigationItemSelectedListener mOnNavigationItemSelectedListener
            = new BottomNavigationView.OnNavigationItemSelectedListener() {

        @Override
        public boolean onNavigationItemSelected(MenuItem item) {
            switch (item.getItemId()) {
                case R.id.navigation_home:
                    return true;
                case R.id.navigation_dashboard:
                    return true;
                case R.id.navigation_notifications:
                    return true;
            }
            return false;
        }
    };


    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        // Check for location permissions
        // If we don't have it, then request for the permission
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            // We already have been granted location permissions
            mMap.setMyLocationEnabled(true);
        } else {
            // We have yet to be granted location permissions
            // The response to this request gets handled by the override function defined below
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, REQUEST_ACCESS_FINE_LOCATION);
        }

        LatLng defaultLoc = new LatLng(DEFAULT_LOCLAT, DEFAULT_LOCLNG);
        mMap.moveCamera(CameraUpdateFactory.newLatLng(defaultLoc));
        mMap.moveCamera(CameraUpdateFactory.zoomTo(DEFAULT_ZOOM));

        // Request and display all the stations within the radius
        requestAndDisplayStations(defaultLoc, 15);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        switch (requestCode) {
            case REQUEST_ACCESS_FINE_LOCATION: {
                if (grantResults.length == 1 && permissions[0].equals(Manifest.permission.ACCESS_FINE_LOCATION) &&
                        grantResults[0] == PackageManager.PERMISSION_GRANTED &&
                        ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                                == PackageManager.PERMISSION_GRANTED) {

                    // Permission was granted
                    mMap.setMyLocationEnabled(true);

                } else {
                    // Permission was denied
                    // We need to figure out what we should do in this case

                }
                return;
            }
            // If we need to request for other permissions, we can check the responses here too
        }
    }

    public void requestAndDisplayStations(LatLng latlng, int radius) {
        // Initialize the request queue
        RequestQueue queue = Volley.newRequestQueue(this);

        // Build the request URL
        String API_Key = getApplicationContext().getString(R.string.nrel_key);
        String urlString = "https://developer.nrel.gov/api/alt-fuel-stations/v1/nearest.json?api_key=" + API_Key +
                "&latitude=" + latlng.latitude + "&longitude=" + latlng.longitude + "&radius=" + radius +
                "&limit=50";

        // Create a request for the json file list of stations
        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(Request.Method.GET, urlString, null,
                new Response.Listener<JSONObject>() {

                    @Override
                    public void onResponse(JSONObject response) {
                        try {
                            // Get the stations out of the response
                            JSONArray jsonArray = response.getJSONArray("fuel_stations");

                            // Add a maker to the map for each station by getting the coordinates and name of each station
                            for (int i = 0; i < jsonArray.length(); i++) {
                                JSONObject station = jsonArray.getJSONObject(i);
                                String stationName = station.getString("station_name");
                                LatLng stationLatLng = new LatLng(station.getDouble("latitude"), station.getDouble("longitude"));

                                mMap.addMarker(new MarkerOptions().position(stationLatLng).title(stationName));
                            }

                        } catch (Exception e) {
                            // Handle Errors Here
                        }
                    }
                }, new Response.ErrorListener() {

            @Override
            public void onErrorResponse(VolleyError error) {
                // Handle Errors Here

            }
        });

        // Add the request for the json file to the request queue
        queue.add(jsonObjectRequest);
    }

    /*---------------------------------------------------------
     *  getPathToStation - Launches Google Maps navigation
     *      with turn-by-turn directions to the desired
     *      charging station from the user's current location.
     *---------------------------------------------------------
     */
    public void getPathToStation(LatLng stationLoc) {
        Intent googleMapIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("google.navigation:q="
                + stationLoc.latitude + "," + stationLoc.longitude));
        googleMapIntent.setPackage("com.google.android.apps.maps");

        /* Prevents crashing if Google Maps isn't installed */
        if (googleMapIntent.resolveActivity(getPackageManager()) != null)
            startActivity(googleMapIntent); /* Launch Google Maps */
    }
}
