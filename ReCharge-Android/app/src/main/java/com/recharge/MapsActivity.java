package com.recharge;

import android.Manifest;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.design.widget.BottomNavigationView;
import android.support.design.widget.BottomSheetBehavior;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.content.Intent;
import android.net.Uri;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;

import android.support.v4.app.FragmentManager;
import android.support.v4.content.ContextCompat;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

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
    private final int PREFERENCES_INTEGER = 0;
    private final int PREFERENCES_BOOLEAN = 1;

    // Request constants
    private final int REQUEST_API_NREL = 1;
    private final int REQUEST_API_SERVER = 2;
    private final String REQUEST_API_SERVER_URL = "http://18.224.1.103:8080/api/status/all";

    // Map and station variables
    private GoogleMap mMap;
    private SupportMapFragment mapFragment;
    private BottomSheetBehavior bottomSheetBehavior;
    private JSONObject stationAvailabilityJSON;
    private JSONObject stationListJSON;
    private HashMap<String, String> stationAvailabilityMap;

    // Preference variables
    private SharedPreferences sp;
    private HashMap<String, Integer> previousIntegerPreferences;
    private HashMap<String, Boolean> previousBooleanPreferences;

    // Location variables
    private FusedLocationProviderClient fusedLocationClient;
    private LatLng lastKnownLocation;

    /**
     * ----------------------------------------------------------------------------------------------------------------
     * onCreate - Executed when the MapsActivity is created.
     * This is the main activity of the app and is executed before any of the UI is created.
     * ----------------------------------------------------------------------------------------------------------------
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
        stationAvailabilityMap = new HashMap<>();

        // Set up the bottom sheet
        View bottomSheet = findViewById(R.id.bottom_sheet);
        bottomSheetBehavior = BottomSheetBehavior.from(bottomSheet);
        bottomSheetBehavior.setPeekHeight(120);
        bottomSheetBehavior.setHideable(true);
        bottomSheetBehavior.setState(BottomSheetBehavior.STATE_HIDDEN);

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        mapFragment = SupportMapFragment.newInstance();
        getSupportFragmentManager().beginTransaction().replace(R.id.fragment_container, mapFragment).commit();
        mapFragment.getMapAsync(this);
    }

    /**
     * ----------------------------------------------------------------------------------------------------------------
     * onMapReady - Executed once the map is available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     * ----------------------------------------------------------------------------------------------------------------
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
        getAndMoveToUserLocation(false, true, false);

        // Set the bottom sheet listeners
        mMap.setOnMarkerClickListener(mOnMarkerClickListener);
        mMap.setOnMapClickListener(mOnMapClickListener);

        // Request and display all the stations within the radius determined by the user
        requestAndDisplayStations(lastKnownLocation, true);
    }

    /**
     * ----------------------------------------------------------------------------------------------------------------
     * bottomNavigationViewListener - Listener for the navigation bar at the bottom of the screen.
     * This determines what should be done based on which button is pressed.
     * ----------------------------------------------------------------------------------------------------------------
     **/
    private BottomNavigationView.OnNavigationItemSelectedListener mOnNavigationItemSelectedListener
            = new BottomNavigationView.OnNavigationItemSelectedListener() {

        @Override
        public boolean onNavigationItemSelected(MenuItem item) {
            // Make sure the bottom sheet is closed
            bottomSheetBehavior.setState(BottomSheetBehavior.STATE_HIDDEN);

            switch (item.getItemId()) {
                case R.id.navigation_map:
                    getSupportFragmentManager().popBackStack("home", FragmentManager.POP_BACK_STACK_INCLUSIVE);
                    getSupportFragmentManager().beginTransaction().replace(R.id.fragment_container, mapFragment).commit();
                    return true;
                case R.id.navigation_station_refresh:
                    getSupportFragmentManager().popBackStack("home", FragmentManager.POP_BACK_STACK_INCLUSIVE);
                    getSupportFragmentManager().beginTransaction().replace(R.id.fragment_container, mapFragment).commit();
                    getAndMoveToUserLocation(true, false, true);
                    return true;
                case R.id.navigation_my_location:
                    getSupportFragmentManager().popBackStack("home", FragmentManager.POP_BACK_STACK_INCLUSIVE);
                    getSupportFragmentManager().beginTransaction().replace(R.id.fragment_container, mapFragment).commit();
                    getAndMoveToUserLocation(true, true, false);
                    return true;
                case R.id.navigation_preferences:
                    Fragment settingsFragment = new SettingsFragment();
                    getSupportFragmentManager().beginTransaction().replace(R.id.fragment_container, settingsFragment).addToBackStack("home").commit();
                    return true;
            }
            return false;
        }
    };

    @Override
    public void onBackPressed() {
        super.onBackPressed();

        Snackbar.make(findViewById(R.id.bottom_sheet_wrapper),
                "This is just a test!",
                Snackbar.LENGTH_LONG).show();
    }

    /**
     * ----------------------------------------------------------------------------------------------------------------
     * getAndMoveToUserLocation - Determines the user's current location and moves the camera to that position.
     * This function verifies that the user has granted the app location permissions and, if so,
     * determines the user's current location and then updates the camera to move to that location.
     * ----------------------------------------------------------------------------------------------------------------
     **/
    private void getAndMoveToUserLocation(boolean tryForPermission, boolean move, boolean refreshStations) {
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
            FragmentActivity activity = mapFragment.getActivity();

            // Check if the activity is null before continuing
            // If it is null, then display an error and return
            if (activity == null) {
                Snackbar.make(findViewById(R.id.bottom_sheet_wrapper),
                        "Location could not be determined. Try again.",
                        Snackbar.LENGTH_LONG).show();
                return;
            }

            // The activity is not null
            // Request the location permissions
            ActivityCompat.requestPermissions(activity,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, REQUEST_ACCESS_FINE_LOCATION);
            return;
        } else {
            return;
        }

        fusedLocationClient.getLastLocation().addOnSuccessListener(location -> {
            // Got last known location. In some rare situations this can be null.
            // Note: location is a Location object
            if (location != null) {
                lastKnownLocation = new LatLng(location.getLatitude(), location.getLongitude());

                if (move) {
                    CameraUpdate cu = CameraUpdateFactory.newLatLngZoom(lastKnownLocation, DEFAULT_ZOOM);
                    mMap.animateCamera(cu);
                }

                if (refreshStations) {
                    requestAndDisplayStations(lastKnownLocation, false);
                }
            } else {
                // Location could not be determined
                Snackbar.make(findViewById(R.id.bottom_sheet_wrapper),
                        "Location could not be determined. Try again.",
                        Snackbar.LENGTH_LONG).show();
            }
        });
    }

    /**
     * ----------------------------------------------------------------------------------------------------------------
     * getRequestPermissionsResult - Executed when the user responds to a permission request
     * This function is called once the user decides to either grant or deny permission for anything we have requested.
     * This is where we can react to that decision appropriately.
     * ----------------------------------------------------------------------------------------------------------------
     **/
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == REQUEST_ACCESS_FINE_LOCATION) {
            if (grantResults.length == 1 && permissions[0].equals(Manifest.permission.ACCESS_FINE_LOCATION) &&
                    grantResults[0] == PackageManager.PERMISSION_GRANTED &&
                    ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                            == PackageManager.PERMISSION_GRANTED) {

                // Permission was granted
                mMap.setMyLocationEnabled(true);
                mMap.getUiSettings().setMyLocationButtonEnabled(false);
                getAndMoveToUserLocation(false, true, false);

            } else {
                // Permission was denied, so display a message to the user
                Snackbar.make(findViewById(R.id.bottom_sheet_wrapper), "Location permissions denied. Try again.",
                        Snackbar.LENGTH_LONG).show();
            }
        }
        // If we need to request for other permissions, we can check the responses here too
    }

    /**
     * ----------------------------------------------------------------------------------------------------------------
     * havePreferencesChanged - Determines if the preferences have changed.
     * This helper function determines whether or not the preferences have changed since the last time they were
     * checked. The response uses one of the response code constants. This also updates the lists of previous
     * preferences.
     * ----------------------------------------------------------------------------------------------------------------
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

            }
            // Handle other preference types here if they exist
        }

        // Return the status of the preferences check
        return status;
    }

    /**
     * ----------------------------------------------------------------------------------------------------------------
     * getPreferenceValue - Returns the stored value for a preference setting.
     * This helper function gets the preference setting currently stored in the previous preferences maps.
     * This function returns an object that can be null or casted to the type variable desired.
     * ----------------------------------------------------------------------------------------------------------------
     **/
    private Object getPreferenceValue(int type, String preferenceName) {
        // Get the value from the preferences or return null if it does not exist
        // Note that the "get" function already returns null if the preference does not exist
        if (type == PREFERENCES_INTEGER) {
            return previousIntegerPreferences.get(preferenceName);
        } else if (type == PREFERENCES_BOOLEAN) {
            return previousBooleanPreferences.get(preferenceName);
        } else {
            return null;
        }
    }

    /**
     * ----------------------------------------------------------------------------------------------------------------
     * requestAndDisplayStations - Requests for the list of stations from the NREL database and updates the map.
     * This function calls the NREL database for the list of stations within the user's specified radius to them.
     * Once the list is retrieved, all the stations are then added to the map for the user to see.
     * The override flag forces the map to update regardless of whether the user's preferences have changed or not
     * ----------------------------------------------------------------------------------------------------------------
     **/
    private void requestAndDisplayStations(LatLng latlng, boolean override) {
        // Check if the user preferences have changed since the last update
        // If not, let the user know the map is already up to date
        int preferenceChangeResult = havePreferencesChanged();
        if (!override && (preferenceChangeResult == PREFERENCES_CHANGE_NONE)) {
            Toast.makeText(this, "Up to date", Toast.LENGTH_SHORT).show();
            // Snackbar.make(findViewById(R.id.bottom_sheet_wrapper), "Up to date", Snackbar.LENGTH_SHORT).show();
            return;
        }

        // Notify the user that the map is updating
        Toast.makeText(this, "Updating stations...", Toast.LENGTH_SHORT).show();
        // Snackbar.make(findViewById(R.id.bottom_sheet_wrapper), "Updating stations...", Snackbar.LENGTH_SHORT).show();

        // If the radius was not updated, then skip requesting the NREL database for a new list
        // Instead, skip to requesting for updates on the availability of the charging stations from our server
        if (!override && (preferenceChangeResult == PREFERENCES_CHANGE_NONRADIUS)) {
            sendAPIRequest(REQUEST_API_SERVER, Request.Method.GET, REQUEST_API_SERVER_URL);
            return;
        }

        // If the preferenceChangeResult is not equal to PREFERENCES_CHANGE_RADIUS here, then something is wrong
        // Display an error and return
        if (preferenceChangeResult != PREFERENCES_CHANGE_RADIUS) {
            Toast.makeText(this, "Error refreshing stations", Toast.LENGTH_SHORT).show();
            return;
        }

        // If the radius was updated, then request for a new list of stations from the NREL database
        // Get the API_Key and build the request URL
        String API_Key = getApplicationContext().getString(R.string.nrel_key);

        // Get the radius
        Object response = getPreferenceValue(PREFERENCES_INTEGER, "seekBar_Radius");

        if (response == null) {
            // An error must have occurred if this is null
            Snackbar.make(findViewById(R.id.bottom_sheet_wrapper), "Could not determine radius. Try again.",
                    Snackbar.LENGTH_LONG).show();
        }

        // The preference object is not null, so we can cast this and continue
        int radius = (int) response;

        String URL = "https://developer.nrel.gov/api/alt-fuel-stations/v1/nearest.json?api_key=" + API_Key +
                "&latitude=" + latlng.latitude + "&longitude=" + latlng.longitude + "&radius=" +
                radius + "&fuel_type=ELEC" + "&limit=200";

        // Send the request to the NREL database for the station list
        sendAPIRequest(REQUEST_API_NREL, Request.Method.GET, URL);
    }

    /**
     * ----------------------------------------------------------------------------------------------------------------
     * sendAPIRequest - Function to send an API call to some URL
     * This function is used to get the station list from the NREL database and also the availability status
     * of charging stations from our server. If we call the NREL database first, then it will call our server
     * right after. After getting the availability update, it will then call the displayStations function
     * to display the results on the map.
     * ----------------------------------------------------------------------------------------------------------------
     **/
    private void sendAPIRequest(int site, int request, String URL) {
        // Initialize the request queue
        RequestQueue queue = Volley.newRequestQueue(this);

        // Create a request for the json file list of stations
        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(request, URL, null, response -> {
            // If this is a response from the NREL database, save the response and then call
            // our server to get station availability
            // Note: response is a JSONObject
            if (site == REQUEST_API_NREL) {
                stationListJSON = response;
                sendAPIRequest(REQUEST_API_SERVER, Request.Method.GET, REQUEST_API_SERVER_URL);

                // If this is a response from our server, save the response and then call displayStations
            } else if (site == REQUEST_API_SERVER) {
                stationAvailabilityJSON = response;

                try {
                    JSONArray jsonArray = stationAvailabilityJSON.getJSONArray("response");

                    for (int i = 0; i < jsonArray.length(); i++) {
                        JSONObject stationAvailability = jsonArray.getJSONObject(i);
                        stationAvailabilityMap.put(stationAvailability.getString("ID"),
                                stationAvailability.getString("AVAILABLE"));
                    }

                } catch (Exception e) {
                    // An error occurred, so display a message to the user
                    Snackbar.make(findViewById(R.id.bottom_sheet_wrapper), "Error retrieving stations. Try again.",
                            Snackbar.LENGTH_LONG).show();
                }

                displayStations();
            }
        }, error -> {
            // Handle Errors Here
            // Note: error is a VolleyError object
        });

        // Add the request for the json file to the request queue
        queue.add(jsonObjectRequest);
    }

    /**
     * ----------------------------------------------------------------------------------------------------------------
     * displayStations - Helper function for requestAndDisplayStations function.
     * This function handles adding, removing, and modifying all station markers on the map.
     * This function relies on the stationListJSON variable, which is updated through requestAndDisplayStations
     * ----------------------------------------------------------------------------------------------------------------
     **/
    private void displayStations() {
        // Clear all the markers on the map
        mMap.clear();

        // Get the user preferences for later use
        Object availablePreference = getPreferenceValue(PREFERENCES_BOOLEAN, "switch_Available");
        Object unknownPreference = getPreferenceValue(PREFERENCES_BOOLEAN, "switch_Unknown");
        Object unavailablePreference = getPreferenceValue(PREFERENCES_BOOLEAN, "switch_Unavailable");

        Object paidPreference = getPreferenceValue(PREFERENCES_BOOLEAN, "switch_Paid");
        Object freePreference = getPreferenceValue(PREFERENCES_BOOLEAN, "switch_Free");

        Object level1 = getPreferenceValue(PREFERENCES_BOOLEAN, "switch_Level1Charging");
        Object level2 = getPreferenceValue(PREFERENCES_BOOLEAN, "switch_Level2Charging");
        Object DC = getPreferenceValue(PREFERENCES_BOOLEAN, "switch_DCCharging");

        if (availablePreference == null || unknownPreference == null || unavailablePreference == null ||
                paidPreference == null || freePreference == null || level1 == null || level2 == null || DC == null) {
            Snackbar.make(findViewById(R.id.bottom_sheet_wrapper), "Preferences could not be loaded. Try again.",
                    Snackbar.LENGTH_LONG).show();
        }

        // Add the stations to the map as new markers
        try {
            // Get the stations list
            JSONArray jsonArray = stationListJSON.getJSONArray("fuel_stations");

            // Add a marker to the map for each station by getting the coordinates and name of each station
            for (int i = 0; i < jsonArray.length(); i++) {
                // Get the station from the station list
                JSONObject station = jsonArray.getJSONObject(i);

                // Get the working status of the charging station
                String workingStatus = station.getString("status_code");
                String expectedDate = station.getString("expected_date");

                // Rewrite the expected date as "None" if there was not one given
                if (expectedDate.equals("null")) {
                    expectedDate = "None";
                }

                // Check if available, unavailable, or unknown
                // Set the default to be unknown
                int chargingAvailability = StationMarkerData.STATUS_UNKNOWN;
                int parkingAvailability = StationMarkerData.STATUS_UNKNOWN;

                // If the working status is not "E", then the charging station is unavailable
                if (!workingStatus.equals("E")) {
                    chargingAvailability = StationMarkerData.STATUS_UNAVAILABLE;
                }

                // Look up the availability status of the station if it is being tracked
                String stationID = station.getString("id");
                String mapLookup;

                try {
                    mapLookup = stationAvailabilityMap.get(stationID);
                } catch (Exception e) {
                    mapLookup = null;
                }

                if (mapLookup != null) {
                    if (mapLookup.equals("Y")) {
                        chargingAvailability = StationMarkerData.STATUS_AVAILABLE;
                    } else {
                        chargingAvailability = StationMarkerData.STATUS_UNAVAILABLE;

                        if (stationID.equals("46844")) {
                            workingStatus = "T";
                        }
                    }
                }

                // Remove the station if it does not match the user settings
                if ((!(boolean) availablePreference && chargingAvailability == StationMarkerData.STATUS_AVAILABLE) ||
                        (!(boolean) unknownPreference && chargingAvailability == StationMarkerData.STATUS_UNKNOWN) ||
                        (!(boolean) unavailablePreference && chargingAvailability == StationMarkerData.STATUS_UNAVAILABLE)) {
                    continue;
                }

                // Check if paid or free
                boolean isPaid = !station.getString("cards_accepted").equals("null");

                if ((!(boolean) paidPreference && isPaid) || (!(boolean) freePreference && !isPaid)) {
                    continue;
                }

                // Check if level 1, level 2, or DC
                boolean hasLevel1 = !station.getString("ev_level1_evse_num").equals("null");
                boolean hasLevel2 = !station.getString("ev_level2_evse_num").equals("null");
                boolean hasDC = !station.getString("ev_dc_fast_num").equals("null");

                if (!(((boolean) level1 && hasLevel1) || ((boolean) level2 && hasLevel2) || ((boolean) DC && hasDC))) {
                    continue;
                }

                String stationName = station.getString("station_name");
                String stationAddress = station.getString("street_address") + ", " +
                        station.getString("city") + ", " + station.getString("state");
                LatLng stationLatLng = new LatLng(station.getDouble("latitude"), station.getDouble("longitude"));

                float[] distance = new float[2];
                Location.distanceBetween(lastKnownLocation.latitude,
                        lastKnownLocation.longitude, stationLatLng.latitude, stationLatLng.longitude,
                        distance);

                // Get station location details
                String locationDetails = station.getString("intersection_directions");

                if (locationDetails.equals("null")) {
                    locationDetails = "Details unavailable";
                }

                // Set up the station marker data
                StationMarkerData info = new StationMarkerData();

                info.setID(stationID);
                info.setAddress(stationAddress);
                info.setDistance(distance[0] * 0.000621371);
                info.setChargingAvailability(chargingAvailability);
                info.setParkingAvailability(parkingAvailability);
                info.setPublicStatus(station.getString("groups_with_access_code"));
                info.setIsPaid(isPaid);
                info.setChargingLevels(hasLevel1, hasLevel2, hasDC);
                info.setWorkingStatus(workingStatus);
                info.setExpectedDate(expectedDate);
                info.setImage("@drawable/ic_ev_station_black_24dp");
                info.setIntersectionDirections(locationDetails);

                // Create the marker and add it to the map
                MarkerOptions markerOptions = new MarkerOptions();

                // Change the color of the marker depending on the charging availability
                BitmapDescriptor bmf;
                if (chargingAvailability == StationMarkerData.STATUS_AVAILABLE) {
                    bmf = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN);
                } else if (chargingAvailability == StationMarkerData.STATUS_UNKNOWN) {
                    bmf = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_ORANGE);
                } else {
                    bmf = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED);
                }

                markerOptions.position((stationLatLng)).title(stationName).snippet(stationAddress);
                Marker m = mMap.addMarker(markerOptions.icon(bmf));
                m.setTag(info);
            }
        } catch (Exception e) {
            // An error occurred, so display a message to the user
            Snackbar.make(findViewById(R.id.bottom_sheet_wrapper), "Error displaying stations. Try again.",
                    Snackbar.LENGTH_LONG).show();
        }

        // Display a message to the user indicating that the stations were updated
        Toast.makeText(this, "Stations updated", Toast.LENGTH_SHORT).show();
        //Snackbar.make(findViewById(R.id.bottom_sheet_wrapper), "Stations updated", Snackbar.LENGTH_INDEFINITE).show();
    }

    /**
     * ----------------------------------------------------------------------------------------------------------------
     * OnMarkerClickListener - Listener for when the markers in the map are clicked (or touched).
     * This brings up the bottom sheet and (after implementation) will display the station info in the sheet.
     * ----------------------------------------------------------------------------------------------------------------
     **/
    private GoogleMap.OnMarkerClickListener mOnMarkerClickListener = new GoogleMap.OnMarkerClickListener() {
        @Override
        public boolean onMarkerClick(Marker marker) {
            // Move the camera to the marker
            CameraUpdate cu = CameraUpdateFactory.newLatLng(marker.getPosition());
            mMap.animateCamera(cu);

            // Update the info in the bottom sheet to be relevant to the marker that was selected
            // Get station marker information
            StationMarkerData stationData = (StationMarkerData) marker.getTag();

            // Ensure that the station data exists
            if (stationData == null) {
                return true;
            }

            // Set title of bottom sheet
            TextView text = findViewById(R.id.bottom_sheet_title);
            text.setText(getString(R.string.bottom_sheet_title, marker.getTitle()));

            // Set address
            text = findViewById(R.id.bottom_sheet_address);
            text.setText(getString(R.string.bottom_sheet_address_value, stationData.getAddress()));
            //text.setText(R.string.bottom_sheet_card_address + stationData.getAddress());

            // Set distance
            text = findViewById(R.id.bottom_sheet_distance);
            //text.setText("Distance to Station: " + stationData.getDistance() + " Miles");
            text.setText(getString(R.string.bottom_sheet_distance_value, stationData.getDistance()));

            // Set charging availability
            text = findViewById(R.id.bottom_sheet_charging_availability);
            if (stationData.getChargingAvailability() == StationMarkerData.STATUS_AVAILABLE) {
                //text.setText("Charging Availability: Yes");
                text.setText(getString(R.string.bottom_sheet_charging_availability_value, "Yes"));
            } else if (stationData.getChargingAvailability() == StationMarkerData.STATUS_UNAVAILABLE) {
                //text.setText("Charging Availability: No");
                text.setText(getString(R.string.bottom_sheet_charging_availability_value, "No"));
            } else {
                //text.setText("Charging Availability: Unknown");
                text.setText(getString(R.string.bottom_sheet_charging_availability_value, "Unknown"));
            }

            // Set parking availability
            text = findViewById(R.id.bottom_sheet_parking_availability);
            if (stationData.getParkingAvailability() == StationMarkerData.STATUS_AVAILABLE) {
                //text.setText("Parking Availability: Yes");
                text.setText(getString(R.string.bottom_sheet_parking_availability_value, "Yes"));
            } else if (stationData.getParkingAvailability() == StationMarkerData.STATUS_UNAVAILABLE) {
                //text.setText("Parking Availability: No");
                text.setText(getString(R.string.bottom_sheet_parking_availability_value, "No"));
            } else {
                //text.setText("Parking Availability: Unknown");
                text.setText(getString(R.string.bottom_sheet_parking_availability_value, "Unknown"));
            }

            // Set public access
            text = findViewById(R.id.bottom_sheet_public_status);
            //text.setText("Public Access: " + stationData.getPublicStatus());
            text.setText(getString(R.string.bottom_sheet_public_access_value, stationData.getPublicStatus()));

            // Set cost status
            text = findViewById(R.id.bottom_sheet_cost_status);
            if (stationData.getIsPaid()) {
                //text.setText("Cost Status: Paid");
                text.setText(getString(R.string.bottom_sheet_cost_status_value, "Paid"));
            } else {
                //text.setText("Cost Status: Free");
                text.setText(getString(R.string.bottom_sheet_cost_status_value, "Free"));
            }

            // Set charging levels
            text = findViewById(R.id.bottom_sheet_charging_levels);
            //text.setText("Charging Levels: " + stationData.getChargingLevels());
            text.setText(getString(R.string.bottom_sheet_charging_levels_value, stationData.getChargingLevels()));

            // Set the ID of the station
            text = findViewById(R.id.bottom_sheet_station_id);
            //text.setText("Station ID: " + stationData.getID());
            text.setText(getString(R.string.bottom_sheet_station_id_value, stationData.getID()));

            // Set working status
            // The status will be "E" if the station is Open
            // The status will be "P" if the station is Planned to be opened
            // The status will be "T" if the station is Temporarily Unavailable
            // Get the "expected_date" value to see the estimated date of opening / reopening
            text = findViewById(R.id.bottom_sheet_working_status);
            if (stationData.getWorkingStatus().equals("E")) {
                //text.setText("Working Status: Operating Normally");
                text.setText(getString(R.string.bottom_sheet_working_status_value, "Operating Normally"));
                text = findViewById(R.id.bottom_sheet_expected_date_label);
                text.setVisibility(View.GONE);
                text = findViewById(R.id.bottom_sheet_expected_date_value);
                text.setVisibility(View.GONE);
            } else if (stationData.getWorkingStatus().equals("P")) {
                //text.setText("Working Status: Planned to be Open");
                text.setText(getString(R.string.bottom_sheet_working_status_value, "Planned to be Open"));
                text = findViewById(R.id.bottom_sheet_expected_date_label);
                text.setVisibility(View.VISIBLE);
                text = findViewById(R.id.bottom_sheet_expected_date_value);
                text.setVisibility(View.VISIBLE);
                //text.setText("Expected Working Date: " + stationData.getExpectedDate());
                text.setText(getString(R.string.bottom_sheet_expected_date_value, stationData.getExpectedDate()));

            } else {
                //text.setText("Working Status: Temporarily Unavailable");
                text.setText(getString(R.string.bottom_sheet_working_status_value, "Temporarily Unavailable"));
                text = findViewById(R.id.bottom_sheet_expected_date_label);
                text.setVisibility(View.VISIBLE);
                text = findViewById(R.id.bottom_sheet_expected_date_value);
                text.setVisibility(View.VISIBLE);
                //text.setText("Expected Working Date: " + stationData.getExpectedDate());
                text.setText(getString(R.string.bottom_sheet_expected_date_value, stationData.getExpectedDate()));
            }

            text = findViewById(R.id.bottom_sheet_address_details);
            text.setText(getString(R.string.bottom_sheet_address_details_value, stationData.getIntersectionDirections()));

            // Set up the location button
            FloatingActionButton getLocation = findViewById(R.id.bottom_sheet_button_location);
            getLocation.setOnClickListener(view -> {
                // Note: view is a View object
                getLocationOfStation(marker);
            });

            // Set up the directions button
            FloatingActionButton getDirections = findViewById(R.id.bottom_sheet_button_directions);
            getDirections.setOnClickListener(view -> {
                // Note: view is a View object
                getPathToStation(marker.getPosition());
            });

            // Make the bottom sheet visible, but leave it collapsed. Let the user decide to open it
            // TODO: Make it so the map size shrinks/grows with the bottom sheet opening/closing and keep the marker centered
            // TODO: Also perhaps make the marker animated to indicate clearly which marker the user is looking at
            bottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);

            // Return true
            return true;
        }
    };

    /**
     * ----------------------------------------------------------------------------------------------------------------
     * OnMapClickListener - Listener for when the map is clicked (or touched).
     * This simply closes the bottom sheet when the user clicks anywhere in the map
     * ----------------------------------------------------------------------------------------------------------------
     **/
    private GoogleMap.OnMapClickListener mOnMapClickListener = new GoogleMap.OnMapClickListener() {
        @Override
        public void onMapClick(LatLng latLng) {
            bottomSheetBehavior.setState(BottomSheetBehavior.STATE_HIDDEN);
        }
    };

    /**
     * ----------------------------------------------------------------------------------------------------------------
     * getPathToStation - Launches Google Maps navigation with turn-by-turn directions to the desired
     * charging station from the user's current location.
     * ----------------------------------------------------------------------------------------------------------------
     **/
    private void getPathToStation(LatLng stationLoc) {
        Intent googleMapIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("google.navigation:q="
                + stationLoc.latitude + "," + stationLoc.longitude));
        googleMapIntent.setPackage("com.google.android.apps.maps");

        /* Launch Google Maps if installed */
        if (googleMapIntent.resolveActivity(getPackageManager()) != null)
            startActivity(googleMapIntent);
    }

    /**
     * ----------------------------------------------------------------------------------------------------------------
     * getLocationOfStation - Launches Google Maps to the location of the desired station charging station
     * ----------------------------------------------------------------------------------------------------------------
     **/
    private void getLocationOfStation(Marker marker) {
        LatLng stationLoc = marker.getPosition();
        Intent googleMapIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("geo:0,0?q="
                + stationLoc.latitude + "," + stationLoc.longitude + "(" + marker.getTitle() + ")"));
        googleMapIntent.setPackage("com.google.android.apps.maps");

        /* Launch Google Maps if installed */
        if (googleMapIntent.resolveActivity(getPackageManager()) != null)
            startActivity(googleMapIntent);
    }
}
