package com.recharge;

import android.Manifest;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.preference.PreferenceManager;
import android.support.design.widget.BottomNavigationView;
import android.support.v4.app.ActivityCompat;
import android.content.Intent;
import android.net.Uri;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;

import android.support.v4.app.FragmentManager;
import android.support.v4.content.ContextCompat;
import android.view.MenuItem;
import android.widget.Toast;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.OnSuccessListener;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.HashMap;


public class MapsActivity extends FragmentActivity implements OnMapReadyCallback {

    // Default constants
    // The default location is currently the Purdue University Engineering Fountain
    private final double DEFAULT_LOCLAT = 40.4286;
    private final double DEFAULT_LOCLNG = -86.9138;
    private final int DEFAULT_ZOOM = 15;

    // Permission constants
    private final int REQUEST_ACCESS_FINE_LOCATION = 1;

    // Preference constants
    private final int PREFERENCES_CHANGE_NONE = 0;
    private final int PREFERENCES_CHANGE_NONRADIUS = 1;
    private final int PREFERENCES_CHANGE_RADIUS = 2;

    // Request constants
    private final int REQUEST_API_NREL = 1;
    private final int REQUEST_API_SERVER = 2;
    private final String REQUEST_API_SERVER_URL = "http://18.224.1.103:8080/api/status/all";

    // Map and station variables
    private GoogleMap mMap;
    private SupportMapFragment mapFragment;
    private JSONObject stationAvailabilityJSON;
    private JSONObject stationListJSON;

    // Preference variables
    private SharedPreferences sp;
    private HashMap<String, Integer> previousIntegerPreferences;
    private HashMap<String, Boolean> previousBooleanPreferences;

    // Location variables
    private FusedLocationProviderClient fusedLocationClient;
    private LatLng lastKnownLocation;

    /**
     * ---------------------------------------------------------
     * onCreate - Executed when the MapsActivity is created.
     * This is the main activity of the app and is executed before any of the UI is created.
     * ---------------------------------------------------------
     **/
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);

        // Set the listener for the navigation bar at the bottom of the screen
        BottomNavigationView navigation = findViewById(R.id.navigationView);
        navigation.setOnNavigationItemSelectedListener(mOnNavigationItemSelectedListener);

        // Set up listener for the location
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        lastKnownLocation = new LatLng(DEFAULT_LOCLAT, DEFAULT_LOCLNG);

        // Set up preference variables
        sp = PreferenceManager.getDefaultSharedPreferences(this);
        previousBooleanPreferences = new HashMap<>();
        previousIntegerPreferences = new HashMap<>();

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
    }

    /**
     * ---------------------------------------------------------
     * onMapReady - Executed once the map is available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     * ---------------------------------------------------------
     **/
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        // Start us at the default location
        LatLng defaultLoc = new LatLng(DEFAULT_LOCLAT, DEFAULT_LOCLNG);
        mMap.moveCamera(CameraUpdateFactory.newLatLng(defaultLoc));
        mMap.moveCamera(CameraUpdateFactory.zoomTo(DEFAULT_ZOOM));
        mMap.getUiSettings().setZoomControlsEnabled(true);

        // Get and move to the user's location if we can
        // Don't ask for permission here. Wait until they hit the My Location Button
        getAndMoveToUserLocation(false, true);

        // Set the custom info windows
        mMap.setInfoWindowAdapter(new CustomInfoWindowGoogleMap(this));

        // Request and display all the stations within the radius determined by the user
        requestAndDisplayStations(lastKnownLocation, true);
    }

    /**
     * ---------------------------------------------------------
     * bottomNavigationViewListener - Listener for the navigation bar at the bottom of the screen.
     * This determines what should be done based on which button is pressed.
     * ---------------------------------------------------------
     **/
    private BottomNavigationView.OnNavigationItemSelectedListener mOnNavigationItemSelectedListener
            = new BottomNavigationView.OnNavigationItemSelectedListener() {

        @Override
        public boolean onNavigationItemSelected(MenuItem item) {
            switch (item.getItemId()) {
                case R.id.navigation_map:
                    getSupportFragmentManager().popBackStack("home", FragmentManager.POP_BACK_STACK_INCLUSIVE);
                    getSupportFragmentManager().beginTransaction().show(mapFragment).commit();
                    return true;
                case R.id.navigation_preferences:
                    Fragment frag = new SettingsFragment();
                    getSupportFragmentManager().beginTransaction().hide(mapFragment).replace(R.id.fragment_container, frag).addToBackStack("home").commit();
                    return true;
                case R.id.navigation_my_location:
                    getAndMoveToUserLocation(true, true);
                    getSupportFragmentManager().popBackStack("home", FragmentManager.POP_BACK_STACK_INCLUSIVE);
                    getSupportFragmentManager().beginTransaction().show(mapFragment).commit();
                    return true;
                case R.id.navigation_station_search:
                    getAndMoveToUserLocation(true, false);
                    getSupportFragmentManager().popBackStack("home", FragmentManager.POP_BACK_STACK_INCLUSIVE);
                    getSupportFragmentManager().beginTransaction().show(mapFragment).commit();
                    requestAndDisplayStations(lastKnownLocation, false);
                    return true;
            }
            return false;
        }
    };

    /**
     * ---------------------------------------------------------
     * getAndMoveToUserLocation - Determines the user's current location and moves the camera to that position.
     * This function verifies that the user has granted the app location permissions and, if so,
     * determines the user's current location and then updates the camera to move to that location.
     * ---------------------------------------------------------
     **/
    private void getAndMoveToUserLocation(boolean tryForPermission, boolean move) {
        // Check for location permissions
        // If we have it, find the user. If not, do not do anything unless the "My Location" button is pressed
        // If we don't have it, then request for the permission
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            // We already have been granted location permissions
            if (!mMap.isMyLocationEnabled()) {
                mMap.setMyLocationEnabled(true);
                mMap.getUiSettings().setMyLocationButtonEnabled(false);
            }
        } else if (tryForPermission) {
            // We have yet to be granted location permissions
            // The response to this request gets handled by the override function defined below
            ActivityCompat.requestPermissions(mapFragment.getActivity(),
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, REQUEST_ACCESS_FINE_LOCATION);
            return;
        } else {
            return;
        }

        fusedLocationClient.getLastLocation()
                .addOnSuccessListener(this, new OnSuccessListener<Location>() {
                    @Override
                    public void onSuccess(Location location) {
                        // Got last known location. In some rare situations this can be null.
                        if (location != null) {
                            lastKnownLocation = new LatLng(location.getLatitude(), location.getLongitude());
                        }
                    }
                });

        if (move) {
            CameraUpdate cu = CameraUpdateFactory.newLatLngZoom(lastKnownLocation, DEFAULT_ZOOM);
            mMap.animateCamera(cu);
        }
    }

    /**
     * ---------------------------------------------------------
     * getRequestPermissionsResult - Executed when the user responds to a permission request
     * This function is called once the user decides to either grant or deny permission for anything we have requested.
     * This is where we can react to that decision appropriately.
     * ---------------------------------------------------------
     **/
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
                    mMap.getUiSettings().setMyLocationButtonEnabled(false);
                    getAndMoveToUserLocation(false, true);

                } else {
                    // Permission was denied
                    // We need to figure out what we should do in this case

                }
                return;
            }
            // If we need to request for other permissions, we can check the responses here too
        }
    }

    /**
     * ---------------------------------------------------------
     * havePreferencesChanged - Determines if the preferences have changed.
     * This helper function determines whether or not the preferences have changed since the last time they were
     * checked. The response uses one of the response code constants. This also updates the lists of previous
     * preferences.
     * ---------------------------------------------------------
     **/
    private int havePreferencesChanged() {
        // Keep track of changes to preferences
        int status = PREFERENCES_CHANGE_NONE;

        // Compare the values of everything in the newPreferences with the values in the lastPreferences
        for (HashMap.Entry<String, ?> i : sp.getAll().entrySet()) {
            // Handle seekBar preferences
            if (i.getKey().startsWith("seekBar")) {
                if (previousIntegerPreferences.get(i.getKey()) != i.getValue()) {
                    if (status < PREFERENCES_CHANGE_RADIUS) {
                        status = PREFERENCES_CHANGE_RADIUS;
                    }

                    previousIntegerPreferences.put(i.getKey(), sp.getInt(i.getKey(), 5));
                }

                // Handle switch preferences
            } else if (i.getKey().startsWith("switch")) {
                if (previousBooleanPreferences.get(i.getKey()) != i.getValue()) {
                    if (status < PREFERENCES_CHANGE_NONRADIUS) {
                        status = PREFERENCES_CHANGE_NONRADIUS;
                    }

                    previousBooleanPreferences.put(i.getKey(), sp.getBoolean(i.getKey(), true));
                }

            } else {
                // Handle other preference types here if they exist
            }
        }

        // Return the status of the preferences check
        return status;
    }

    /**
     * ---------------------------------------------------------
     * requestAndDisplayStations - Requests for the list of stations from the NREL database and updates the map.
     * This function calls the NREL database for the list of stations within the user's specified radius to them.
     * Once the list is retrieved, all the stations are then added to the map for the user to see.
     * The override flag forces the map to update regardless of whether the user's preferences have changed or not
     * ---------------------------------------------------------
     **/
    private void requestAndDisplayStations(LatLng latlng, boolean override) {
        // Check if the user preferences have changed since the last update
        // If not, let the user know the map is already up to date
        int preferenceChangeResult = havePreferencesChanged();
        if (!override && (preferenceChangeResult == PREFERENCES_CHANGE_NONE)) {
            Toast.makeText(this, "Up to date", Toast.LENGTH_SHORT).show();
            return;
        }

        // Notify the user that the map is updating
        Toast.makeText(this, "Updating stations...", Toast.LENGTH_SHORT).show();

        // If the radius was not updated, then skip requesting the NREL database for a new list
        // Instead, skip to requesting for updates on the availability of the charging stations from our server
        if (!override && (preferenceChangeResult == PREFERENCES_CHANGE_NONRADIUS)) {
            sendAPIRequest(REQUEST_API_SERVER, Request.Method.GET, REQUEST_API_SERVER_URL);
            return;
        }

        // If the radius was updated, then request for a new list of stations from the NREL database
        // Get the API_Key and build the request URL
        String API_Key = getApplicationContext().getString(R.string.nrel_key);
        String URL = "https://developer.nrel.gov/api/alt-fuel-stations/v1/nearest.json?api_key=" + API_Key +
                "&latitude=" + latlng.latitude + "&longitude=" + latlng.longitude + "&radius=" +
                previousIntegerPreferences.get("seekBar_Radius") + "&fuel_type=ELEC" + "&limit=100";

        // Send the request to the NREL database for the station list
        sendAPIRequest(REQUEST_API_NREL, Request.Method.GET, URL);
    }

    /**
     * ---------------------------------------------------------
     * sendAPIRequest - Function to send an API call to some URL
     * This function is used to get the station list from the NREL database and also the availability status
     * of charging stations from our server. If we call the NREL database first, then it will call our server
     * right after. After getting the availability update, it will then call the displayStations function
     * to display the results on the map.
     * ---------------------------------------------------------
     **/
    private void sendAPIRequest(int site, int request, String URL) {
        // Initialize the request queue
        RequestQueue queue = Volley.newRequestQueue(this);

        // Create a request for the json file list of stations
        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(Request.Method.GET, URL, null,
                new Response.Listener<JSONObject>() {

                    @Override
                    public void onResponse(JSONObject response) {
                        // If this is a response from the NREL database, save the response and then call
                        // our server to get station availability
                        if (site == REQUEST_API_NREL) {
                            stationListJSON = response;
                            sendAPIRequest(REQUEST_API_SERVER, Request.Method.GET, REQUEST_API_SERVER_URL);

                            // If this is a response from our server, save the response and then call displayStations
                        } else if (site == REQUEST_API_SERVER) {
                            stationAvailabilityJSON = response;
                            displayStations();
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

    /**
     * ---------------------------------------------------------
     * displayStations - Helper function for requestAndDisplayStations function.
     * This function handles adding, removing, and modifying all station markers on the map.
     * This function relies on the stationListJSON variable, which is updated through requestAndDisplayStations
     * ---------------------------------------------------------
     **/
    private void displayStations() {
        // Clear all the markers on the map
        mMap.clear();

        // Add the stations to the map as new markers
        try {
            // Get the stations list
            JSONArray jsonArray = stationListJSON.getJSONArray("fuel_stations");

            // Add a marker to the map for each station by getting the coordinates and name of each station
            for (int i = 0; i < jsonArray.length(); i++) {
                // Get the station from the station list
                JSONObject station = jsonArray.getJSONObject(i);

                // Check if the station matches the user preferences
                // Check if available, unavailable, or unknown
                // TODO: Use station availability response from our server

                // Check if paid or free
                boolean paid = previousBooleanPreferences.get("switch_Paid");
                boolean free = previousBooleanPreferences.get("switch_Free");
                String cardsAccepted = station.getString("cards_accepted");

                if ((!paid && !cardsAccepted.equals("null")) || (!free && cardsAccepted.equals("null"))) {
                    continue;
                }

                // Check if level 1, level 2, or DC
                boolean level1 = previousBooleanPreferences.get("switch_Level1Charging");
                boolean level2 = previousBooleanPreferences.get("switch_Level2Charging");
                boolean DC = previousBooleanPreferences.get("switch_DCCharging");
                String hasLevel1 = station.getString("ev_level1_evse_num");
                String hasLevel2 = station.getString("ev_level2_evse_num");
                String hasDC = station.getString("ev_dc_fast_num");

                if ((!level1 && !hasLevel1.equals("null")) || (!level2 && !hasLevel2.equals("null")) || (!DC && !hasDC.equals("null"))) {
                    continue;
                }

                String stationName = station.getString("station_name");
                String stationAddress = station.getString("street_address") + ", " +
                        station.getString("city") + ", " + station.getString("state");
                LatLng stationLatLng = new LatLng(station.getDouble("latitude"), station.getDouble("longitude"));

                InfoWindowData info = new InfoWindowData();
                float distance[] = new float[2];
                Location.distanceBetween(lastKnownLocation.latitude,
                        lastKnownLocation.longitude, stationLatLng.latitude, stationLatLng.longitude,
                        distance);
                info.setDistance(distance[0]);

                info.setPublicStatus(station.getString("groups_with_access_code"));
                info.setChargingAvailability(false);
                info.setParkingAvailability(false);
                info.setImage("@drawable/ic_ev_station_black_24dp");

                MarkerOptions markerOptions = new MarkerOptions();
                markerOptions.position((stationLatLng)).title(stationName).snippet(stationAddress);
                Marker m = mMap.addMarker(markerOptions);
                m.setTag(info);
            }
        } catch (Exception e) {
            // Handle Errors Here
        }

        Toast.makeText(this, "Stations updated", Toast.LENGTH_SHORT).show();
    }

    /**
     * ---------------------------------------------------------
     * getPathToStation - Launches Google Maps navigation
     * with turn-by-turn directions to the desired
     * charging station from the user's current location.
     * ---------------------------------------------------------
     **/
    private void getPathToStation(LatLng stationLoc) {
        Intent googleMapIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("google.navigation:q="
                + stationLoc.latitude + "," + stationLoc.longitude));
        googleMapIntent.setPackage("com.google.android.apps.maps");

        /* Launch Google Maps if installed */
        if (googleMapIntent.resolveActivity(getPackageManager()) != null)
            startActivity(googleMapIntent);
    }
}
