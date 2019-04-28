package com.recharge;

import android.Manifest;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.os.Build;
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
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;
import android.support.v4.content.ContextCompat;
import android.view.MenuItem;
import android.view.View;
import android.widget.Switch;
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
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;


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

    // Notifications constants
    private final String NOTIFICATIONS_NEARBY_CHANNEL = "NEARBY_STATIONS_CHANNEL_ID";
    private final String NOTIFICATIONS_TRACKING_CHANNEL = "TRACKING_STATIONS_CHANNEL_ID";
    private static ScheduledExecutorService repeater;
    private static boolean firstRun;

    // Request constants
    private final int REQUEST_API_NREL = 1;
    private final int REQUEST_API_SERVER = 2;
    private final String REQUEST_API_SERVER_URL = "http://18.224.1.103:8080";
    private final String REQUEST_API_SERVER_GET = "/api/status/all";
    private final String REQUEST_API_SERVER_POST = "/api/history/specific";

    // Map and station variables
    private GoogleMap mMap;
    private SupportMapFragment mapFragment;
    private BottomSheetBehavior bottomSheetBehavior;
    private JSONObject stationAvailabilityJSON;
    private HashMap<String, Marker> stationMarkerMap;

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
        stationMarkerMap = new HashMap<>();

        // Set up the bottom sheet
        View bottomSheet = findViewById(R.id.bottom_sheet);
        bottomSheetBehavior = BottomSheetBehavior.from(bottomSheet);
        bottomSheetBehavior.setPeekHeight(120);
        bottomSheetBehavior.setHideable(true);
        bottomSheetBehavior.setState(BottomSheetBehavior.STATE_HIDDEN);

        // Set up the notification system
        setupNotificationSystem();
        repeater = Executors.newSingleThreadScheduledExecutor();
        firstRun = true;

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
     * setupNotificationSystem - Function used to initialize the notification system
     * This creates the required notification channels on Android if the Android version is new enough to require it
     * ----------------------------------------------------------------------------------------------------------------
     **/
    private void setupNotificationSystem() {
        // Create the NotificationChannel, but only on API 26+ because
        // the NotificationChannel class is new and not in the support library
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = getString(R.string.notifications_stations_nearby_name);
            String description = getString(R.string.notifications_stations_nearby_description);
            int importance = NotificationManager.IMPORTANCE_HIGH;

            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            NotificationChannel channel = new NotificationChannel(NOTIFICATIONS_NEARBY_CHANNEL, name, importance);
            channel.setDescription(description);
            channel.enableLights(true);
            channel.setLightColor(Color.YELLOW);
            channel.enableVibration(true);

            // Register the channel with the system; you can't change the importance
            // or other notification behaviors after this
            notificationManager.createNotificationChannel(channel);

            name = getString(R.string.notifications_stations_tracking_name);
            description = getString(R.string.notifications_stations_tracking_description);
            channel = new NotificationChannel(NOTIFICATIONS_TRACKING_CHANNEL, name, importance);
            channel.setDescription(description);
            channel.enableLights(true);
            channel.setLightColor(Color.YELLOW);
            channel.enableVibration(true);

            // Register the channel with the system
            notificationManager.createNotificationChannel(channel);
        }
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

    /**
     * ----------------------------------------------------------------------------------------------------------------
     * onBackPressed - Listener for when the back button is pressed by the user
     * This can be used to handle any action that we wish when the user presses the back button
     * ----------------------------------------------------------------------------------------------------------------
     **/
    @Override
    public void onBackPressed() {
        super.onBackPressed();
        /*
        Snackbar.make(findViewById(R.id.bottom_sheet_wrapper),
                "This is just a test!",
                Snackbar.LENGTH_LONG).show();
        */
    }

    /**
     * ----------------------------------------------------------------------------------------------------------------
     * sendNotification - Function used to display a push notification to the user
     * This can be used to handle any action that we wish when the user presses the back button
     * ----------------------------------------------------------------------------------------------------------------
     **/
    public void sendNotification(String title, String content, String ChannelID) {
        // Create an explicit intent for an Activity in your app
        Intent intent = new Intent(this, MapsActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);

        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent, 0);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, ChannelID)
                .setSmallIcon(R.drawable.ic_stat_name)
                .setContentTitle(title)
                .setContentText(content)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(content))
                .setPriority(NotificationCompat.PRIORITY_MAX)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                // Set the intent that will fire when the user taps the notification
                .setContentIntent(pendingIntent)
                .setAutoCancel(true);

        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(this);

        // notificationId is a unique int for each notification that you must define
        notificationManager.notify(0, builder.build());
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
            // Skip those with the "tracking" prefix as they are not used for filtering
        }

        // Return the status of the preferences check
        return status;
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
            sendAPIRequest(REQUEST_API_SERVER, Request.Method.GET, REQUEST_API_SERVER_URL, false);
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

        // Get the radius from the preferences
        Integer radius = previousIntegerPreferences.get("seekBar_Radius");

        // If the radius preference does not exist, then return an error
        if (radius == null) {
            // An error must have occurred if this is null
            Snackbar.make(findViewById(R.id.bottom_sheet_wrapper), "Could not determine radius. Try again.",
                    Snackbar.LENGTH_LONG).show();
            return;
        }

        String URL = "https://developer.nrel.gov/api/alt-fuel-stations/v1/nearest.json?api_key=" + API_Key +
                "&latitude=" + latlng.latitude + "&longitude=" + latlng.longitude + "&radius=" +
                radius + "&fuel_type=ELEC" + "&limit=200";

        // Send the request to the NREL database for the station list
        sendAPIRequest(REQUEST_API_NREL, Request.Method.GET, URL, false);
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
    private void sendAPIRequest(int site, int request, String URL, boolean repeatingCall) {
        // Initialize the request queue
        RequestQueue queue = Volley.newRequestQueue(this);

        // Get the full URL we are sending
        String sendURL;
        JSONObject jsonRequestObject = null;
        if (site == REQUEST_API_SERVER && request == Request.Method.GET) {
            sendURL = URL + REQUEST_API_SERVER_GET;
        } else if (site == REQUEST_API_SERVER && request == Request.Method.POST) {
            sendURL = URL + REQUEST_API_SERVER_POST;

            HashMap<String, String> params = new HashMap<>();
            params.put("ID", "76232");
            jsonRequestObject = new JSONObject(params);

        } else {
            sendURL = URL;
        }

        // Create a request for the json file list of stations
        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(request, sendURL, jsonRequestObject, response -> {
            // If this is a response from the NREL database, save the response and then call
            // our server to get station availability
            // Note: response is a JSONObject
            if (site == REQUEST_API_NREL) {
                parseStations(response);
                sendAPIRequest(REQUEST_API_SERVER, Request.Method.GET, REQUEST_API_SERVER_URL, false);

                // If this is a response from our server, save the response and then call displayStations
            } else if (site == REQUEST_API_SERVER && request == Request.Method.GET) {
                stationAvailabilityJSON = response;
                boolean updated = false;

                try {
                    // Get the station array from our server response
                    JSONArray jsonArray = stationAvailabilityJSON.getJSONArray("response");

                    // For every station in the station array, update the availability status
                    for (int i = 0; i < jsonArray.length(); i++) {
                        JSONObject stationAvailability = jsonArray.getJSONObject(i);

                        // Get the marker of the station we are updating
                        Marker marker = stationMarkerMap.get(stationAvailability.getString("ID"));

                        // If the marker does not exist, then skip this entry
                        if (marker == null) {
                            continue;
                        }

                        // Get the associated data of the marker
                        StationMarkerData stationData = (StationMarkerData) marker.getTag();

                        // If the data does not exist, then skip this entry
                        if (stationData == null) {
                            continue;
                        }

                        int beforeCharging = stationData.getChargingAvailability();
                        int beforeParking = stationData.getParkingAvailability();

                        stationData.setChargingAvailabilityString(stationAvailability.getString("AVAILABLE"));
                        stationData.setParkingAvailabilityString(stationAvailability.getString("REMAINING_SPACE"));

                        boolean chargingChanged = (beforeCharging != stationData.getChargingAvailability());
                        boolean parkingChanged = (beforeParking != stationData.getParkingAvailability());

                        if (chargingChanged || parkingChanged) {
                            // Mark that an update is needed
                            updated = true;

                            // Notify the user of this change if this is not the first run
                            // Only run if the user preferences match
                            if (!firstRun) {
                                // Get the global user preference
                                boolean nearbyTracking = sp.getBoolean("tracking_NearbyNotifications", false);

                                // If the user is tracking this specific station, then notify them of changes
                                if (stationData.getChargingTrackingStatus() || stationData.getParkingTrackingStatus()) {
                                    if (stationData.getChargingAvailability() == StationMarkerData.STATUS_AVAILABLE && stationData.getParkingAvailability() == StationMarkerData.STATUS_AVAILABLE && stationData.getChargingTrackingStatus() && stationData.getParkingTrackingStatus()) {
                                        sendNotification("Tracked Station Updated",
                                                "Your tracked station \"" + stationData.getName() + "\" is now available for both charging and parking!", NOTIFICATIONS_TRACKING_CHANNEL);
                                    } else if (chargingChanged && stationData.getChargingAvailability() == StationMarkerData.STATUS_AVAILABLE && stationData.getChargingTrackingStatus()) {
                                        sendNotification("Tracked Station Updated",
                                                "Your tracked station \"" + stationData.getName() + "\" is now available for charging!", NOTIFICATIONS_TRACKING_CHANNEL);
                                    } else if (parkingChanged && stationData.getParkingAvailability() == StationMarkerData.STATUS_AVAILABLE && stationData.getParkingTrackingStatus()) {
                                        sendNotification("Tracked Station Updated",
                                                "Your tracked station \"" + stationData.getName() + "\" is now available for parking!", NOTIFICATIONS_TRACKING_CHANNEL);
                                    }

                                    // If the station is within one and a half miles of the user, then notify them if they chose to have nearby stations notify them
                                } else if (nearbyTracking && stationData.getDistance(lastKnownLocation) < 1.5) {
                                    if (stationData.getChargingAvailability() == StationMarkerData.STATUS_AVAILABLE && stationData.getParkingAvailability() == StationMarkerData.STATUS_AVAILABLE) {
                                        sendNotification("Nearby Station Updated", "The nearby station \"" + stationData.getName() + "\" is now available for both charging and parking!", NOTIFICATIONS_NEARBY_CHANNEL);
                                    } else if (chargingChanged && stationData.getChargingAvailability() == StationMarkerData.STATUS_AVAILABLE) {
                                        sendNotification("Nearby Station Updated", "The nearby station \"" + stationData.getName() + "\" is now available for charging!", NOTIFICATIONS_NEARBY_CHANNEL);
                                    } else if (parkingChanged && stationData.getParkingAvailability() == StationMarkerData.STATUS_AVAILABLE) {
                                        sendNotification("Nearby Station Updated", "The nearby station \"" + stationData.getName() + "\" is now available for parking!", NOTIFICATIONS_NEARBY_CHANNEL);
                                    }
                                }
                            }
                        }
                    }

                } catch (JSONException e) {
                    // An error occurred, so display a message to the user
                    Snackbar.make(findViewById(R.id.bottom_sheet_wrapper), "Error retrieving stations. Try again.",
                            Snackbar.LENGTH_LONG).show();
                }

                // Only update the display if the station availabilities changed
                if (updated) {
                    displayStations(repeatingCall);
                }
            } else if (site == REQUEST_API_SERVER && request == Request.Method.POST) {
                // TODO: Handle POST response
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
     * parseStations - Helper function for requestAndDisplayStations function.
     * This function handles parsing all of the stations from the response JSON into a hashtable for future use.
     * ----------------------------------------------------------------------------------------------------------------
     **/
    private void parseStations(JSONObject stationListJSON) {
        // If this function is called, then we are recreating the hashtable and map
        // So, clear the current hashtable and map
        stationMarkerMap.clear();
        mMap.clear();

        try {
            // Get the list of stations
            JSONArray jsonArray = stationListJSON.getJSONArray("fuel_stations");

            // Iterate across every station and create a marker for them
            // For each marker, create a StationMarkerData object to hold all of the information
            // Afterwards, save each marker in the hashtable
            for (int i = 0; i < jsonArray.length(); i++) {
                // Get the station from the list of stations and create a StationMarkerObject for it
                StationMarkerData stationData = new StationMarkerData(jsonArray.getJSONObject(i), getApplicationContext());

                // Store the StationMarkerData object in a marker
                // Add the marker to the map, but make it invisible for now
                MarkerOptions markerOptions = new MarkerOptions();
                markerOptions.position(stationData.getLocation());
                markerOptions.visible(false);
                Marker marker = mMap.addMarker(markerOptions);
                marker.setTag(stationData);

                // Save the marker in the hashtable
                stationMarkerMap.put(stationData.getID(), marker);
            }

        } catch (JSONException e) {
            // An error occurred, so display a message to the user
            Snackbar.make(findViewById(R.id.bottom_sheet_wrapper), "Error parsing stations. Try again.",
                    Snackbar.LENGTH_LONG).show();
        }
    }

    /**
     * ----------------------------------------------------------------------------------------------------------------
     * displayStations - Helper function for requestAndDisplayStations function.
     * This function handles adding, removing, and modifying all station markers on the map.
     * This function relies on the stationListJSON variable, which is updated through requestAndDisplayStations
     * ----------------------------------------------------------------------------------------------------------------
     **/
    private void displayStations(boolean repeatingCall) {
        // Get the user preferences for later use
        Boolean availablePreference = previousBooleanPreferences.get("switch_Available");
        Boolean unknownPreference = previousBooleanPreferences.get("switch_Unknown");
        Boolean unavailablePreference = previousBooleanPreferences.get("switch_Unavailable");

        Boolean paidPreference = previousBooleanPreferences.get("switch_Paid");
        Boolean freePreference = previousBooleanPreferences.get("switch_Free");

        Boolean level1 = previousBooleanPreferences.get("switch_Level1Charging");
        Boolean level2 = previousBooleanPreferences.get("switch_Level2Charging");
        Boolean DC = previousBooleanPreferences.get("switch_DCCharging");

        if (availablePreference == null || unknownPreference == null || unavailablePreference == null ||
                paidPreference == null || freePreference == null || level1 == null || level2 == null || DC == null) {
            Snackbar.make(findViewById(R.id.bottom_sheet_wrapper), "Preferences could not be loaded. Try again.",
                    Snackbar.LENGTH_LONG).show();
            return;
        }

        // Modify station marker visibility based on the user preferences
        // All of the markers on the map are stored in the stationMarkerMap
        for (Marker i : stationMarkerMap.values()) {
            // Get the tag of the marker
            StationMarkerData stationData = (StationMarkerData) i.getTag();

            if (stationData == null) {
                // An error occurred, so display a message to the user
                Snackbar.make(findViewById(R.id.bottom_sheet_wrapper), "Error displaying stations. Try again.",
                        Snackbar.LENGTH_LONG).show();
                return;
            }

            // Make the station marker invisible and move on to the next station if it does not match the preferences
            if ((!availablePreference && stationData.getChargingAvailability() == StationMarkerData.STATUS_AVAILABLE) ||
                    (!unknownPreference && stationData.getChargingAvailability() == StationMarkerData.STATUS_UNKNOWN) ||
                    (!unavailablePreference && stationData.getChargingAvailability() == StationMarkerData.STATUS_UNAVAILABLE)) {
                i.setVisible(false);
                continue;
            }

            // Make the station marker invisible and move on to the next station if it does not match the preferences
            if ((!paidPreference && stationData.getIsPaid()) || (!freePreference && !stationData.getIsPaid())) {
                i.setVisible(false);
                continue;
            }

            // Make the station marker invisible and move on to the next station if it does not match the preferences
            if (!((level1 && stationData.hasLevel()) || (level2 && stationData.hasLeve2()) || (DC && stationData.hasDC()))) {
                i.setVisible(false);
                continue;
            }

            // Set the color of the marker icon depending on the charging and parking availability
            int charging = stationData.getChargingAvailability();
            int parking = stationData.getParkingAvailability();
            boolean paid = stationData.getIsPaid();
            if (!paid && charging == StationMarkerData.STATUS_AVAILABLE && parking == StationMarkerData.STATUS_AVAILABLE) {
                Bitmap bitmap = ((BitmapDrawable) getResources().getDrawable(R.drawable.location_pin_available)).getBitmap();
                Bitmap smallMarker = Bitmap.createScaledBitmap(bitmap, 128, 128, false);
                i.setIcon(BitmapDescriptorFactory.fromBitmap(smallMarker));

            } else if (!paid && charging == StationMarkerData.STATUS_AVAILABLE && parking == StationMarkerData.STATUS_UNAVAILABLE) {
                Bitmap bitmap = ((BitmapDrawable) getResources().getDrawable(R.drawable.location_pin_no_parking)).getBitmap();
                Bitmap smallMarker = Bitmap.createScaledBitmap(bitmap, 128, 128, false);
                i.setIcon(BitmapDescriptorFactory.fromBitmap(smallMarker));

            } else if (!paid && charging == StationMarkerData.STATUS_UNAVAILABLE && parking == StationMarkerData.STATUS_AVAILABLE) {
                Bitmap bitmap = ((BitmapDrawable) getResources().getDrawable(R.drawable.location_pin_no_charging)).getBitmap();
                Bitmap smallMarker = Bitmap.createScaledBitmap(bitmap, 128, 128, false);
                i.setIcon(BitmapDescriptorFactory.fromBitmap(smallMarker));

            } else if (!paid && charging == StationMarkerData.STATUS_UNAVAILABLE && parking == StationMarkerData.STATUS_UNAVAILABLE) {
                Bitmap bitmap = ((BitmapDrawable) getResources().getDrawable(R.drawable.location_pin_no_parking_charging)).getBitmap();
                Bitmap smallMarker = Bitmap.createScaledBitmap(bitmap, 128, 128, false);
                i.setIcon(BitmapDescriptorFactory.fromBitmap(smallMarker));

            } else if (paid && charging == StationMarkerData.STATUS_AVAILABLE && parking == StationMarkerData.STATUS_AVAILABLE) {
                Bitmap bitmap = ((BitmapDrawable) getResources().getDrawable(R.drawable.location_pin_paid_available)).getBitmap();
                Bitmap smallMarker = Bitmap.createScaledBitmap(bitmap, 128, 128, false);
                i.setIcon(BitmapDescriptorFactory.fromBitmap(smallMarker));

            } else if (paid && charging == StationMarkerData.STATUS_AVAILABLE && parking == StationMarkerData.STATUS_UNAVAILABLE) {
                Bitmap bitmap = ((BitmapDrawable) getResources().getDrawable(R.drawable.location_pin_paid_no_parking)).getBitmap();
                Bitmap smallMarker = Bitmap.createScaledBitmap(bitmap, 128, 128, false);
                i.setIcon(BitmapDescriptorFactory.fromBitmap(smallMarker));

            } else if (paid && charging == StationMarkerData.STATUS_UNAVAILABLE && parking == StationMarkerData.STATUS_AVAILABLE) {
                Bitmap bitmap = ((BitmapDrawable) getResources().getDrawable(R.drawable.location_pin_paid_no_charging)).getBitmap();
                Bitmap smallMarker = Bitmap.createScaledBitmap(bitmap, 128, 128, false);
                i.setIcon(BitmapDescriptorFactory.fromBitmap(smallMarker));

            } else if (paid && charging == StationMarkerData.STATUS_UNAVAILABLE && parking == StationMarkerData.STATUS_UNAVAILABLE) {
                Bitmap bitmap = ((BitmapDrawable) getResources().getDrawable(R.drawable.location_pin_paid_no_parking_charging)).getBitmap();
                Bitmap smallMarker = Bitmap.createScaledBitmap(bitmap, 128, 128, false);
                i.setIcon(BitmapDescriptorFactory.fromBitmap(smallMarker));

            } else {
                Bitmap bitmap = ((BitmapDrawable) getResources().getDrawable(R.drawable.location_pin_not_tracking)).getBitmap();
                Bitmap smallMarker = Bitmap.createScaledBitmap(bitmap, 128, 128, false);
                i.setIcon(BitmapDescriptorFactory.fromBitmap(smallMarker));
            }

            // If a station is not currently in operation, override its icon
            // Use an icon that clearly indicates that a station is not in working order
            if (!stationData.getWorkingStatus().equals("E")) {
                Bitmap bitmap = ((BitmapDrawable) getResources().getDrawable(R.drawable.location_pin_not_in_service)).getBitmap();
                Bitmap smallMarker = Bitmap.createScaledBitmap(bitmap, 128, 128, false);
                i.setIcon(BitmapDescriptorFactory.fromBitmap(smallMarker));
            }

            // Make the station visible
            i.setVisible(true);
        }

        // Display a message to the user indicating that the stations were updated
        if (!repeatingCall) {
            Toast.makeText(this, "Stations updated", Toast.LENGTH_SHORT).show();
        }
        //Snackbar.make(findViewById(R.id.bottom_sheet_wrapper), "Stations updated", Snackbar.LENGTH_INDEFINITE).show();

        // After the first run, start running this code regularly
        if (firstRun) {
            firstRun = false;
            repeater.scheduleWithFixedDelay(runnableCode, 6, 6, TimeUnit.SECONDS);
        }
    }

    /**
     * ----------------------------------------------------------------------------------------------------------------
     * Runnable - Repeatable code used to continuously keep checking for station availability updates
     * This also avoids alerting the user that the stations were updated unless something actually changed
     * ----------------------------------------------------------------------------------------------------------------
     **/
    private Runnable runnableCode = new Runnable() {
        @Override
        public void run() {
            // Do something here on the main thread
            sendAPIRequest(REQUEST_API_SERVER, Request.Method.GET, REQUEST_API_SERVER_URL, true);
        }
    };

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
            // If not, indicate that we handled the event and do nothing else
            if (stationData == null) {
                return true;
            }

            // Set title of bottom sheet
            TextView text = findViewById(R.id.bottom_sheet_title);
            text.setText(getString(R.string.bottom_sheet_title, stationData.getName()));

            // Set address
            text = findViewById(R.id.bottom_sheet_address);
            text.setText(getString(R.string.bottom_sheet_address_value, stationData.getAddress()));

            // Set distance
            text = findViewById(R.id.bottom_sheet_distance);
            text.setText(getString(R.string.bottom_sheet_distance_value, stationData.getDistance(lastKnownLocation)));

            // Set the location details
            text = findViewById(R.id.bottom_sheet_location_details);
            text.setText(getString(R.string.bottom_sheet_location_details_value, stationData.getLocationDetails()));

            // Set public access
            text = findViewById(R.id.bottom_sheet_public_status);
            text.setText(getString(R.string.bottom_sheet_public_access_value, stationData.getPublicStatus()));

            // Set cost status
            text = findViewById(R.id.bottom_sheet_cost_status);
            text.setText(getString(R.string.bottom_sheet_cost_status_value, stationData.getIsPaidText()));

            // Set charging levels
            text = findViewById(R.id.bottom_sheet_charging_levels);
            text.setText(getString(R.string.bottom_sheet_charging_levels_value, stationData.getAllChargingLevels()));

            // Set the ID of the station
            text = findViewById(R.id.bottom_sheet_station_id);
            text.setText(getString(R.string.bottom_sheet_station_id_value, stationData.getID()));

            // Set charging availability
            text = findViewById(R.id.bottom_sheet_charging_availability);
            text.setText(getString(R.string.bottom_sheet_charging_availability_value, stationData.getChargingAvailabilityText()));

            // Set parking availability
            text = findViewById(R.id.bottom_sheet_parking_availability);
            text.setText(getString(R.string.bottom_sheet_parking_availability_value, stationData.getParkingAvailabilityText()));

            // Set working status
            text = findViewById(R.id.bottom_sheet_working_status);
            text.setText(getString(R.string.bottom_sheet_working_status_value, stationData.getWorkingStatusText()));

            // Set the "expected_date" value to see the estimated date of opening / reopening
            text = findViewById(R.id.bottom_sheet_expected_date_label);
            if (stationData.getWorkingStatus().equals("E")) {
                text.setVisibility(View.GONE);
                text = findViewById(R.id.bottom_sheet_expected_date_value);
                text.setVisibility(View.GONE);
            } else if (stationData.getWorkingStatus().equals("P")) {
                text.setVisibility(View.VISIBLE);
                text = findViewById(R.id.bottom_sheet_expected_date_value);
                text.setVisibility(View.VISIBLE);
                text.setText(getString(R.string.bottom_sheet_expected_date_value, stationData.getExpectedDate()));
            } else {
                text.setVisibility(View.VISIBLE);
                text = findViewById(R.id.bottom_sheet_expected_date_value);
                text.setVisibility(View.VISIBLE);
                text.setText(getString(R.string.bottom_sheet_expected_date_value, stationData.getExpectedDate()));
            }

            // Get the station statistics
            sendAPIRequest(REQUEST_API_SERVER, Request.Method.POST, REQUEST_API_SERVER_URL, false);

            // Set up the charging status tracking switch
            Switch trackChargingSwitch = findViewById(R.id.bottom_sheet_track_charging);
            trackChargingSwitch.setChecked(stationData.getChargingTrackingStatus());
            trackChargingSwitch.setOnClickListener(view -> {
                stationData.setChargingTrackingStatus(!stationData.getChargingTrackingStatus());
            });

            // Set up the parking status tracking switch
            Switch trackParkingSwitch = findViewById(R.id.bottom_sheet_track_parking);
            trackParkingSwitch.setChecked(stationData.getParkingTrackingStatus());
            trackParkingSwitch.setOnClickListener(view -> {
                stationData.setParkingTrackingStatus(!stationData.getParkingTrackingStatus());
            });

            // Set up the location button
            FloatingActionButton locationButton = findViewById(R.id.bottom_sheet_button_location);
            locationButton.setOnClickListener(view -> {
                // Note: view is a View object
                getLocationOfStation(marker);
            });

            // Set up the directions button
            FloatingActionButton directionsButton = findViewById(R.id.bottom_sheet_button_directions);
            directionsButton.setOnClickListener(view -> {
                // Note: view is a View object
                getPathToStation(marker.getPosition());
            });

            // Make the bottom sheet visible, but leave it collapsed. Let the user decide to open it
            // TODO: Make it so the map size shrinks/grows with the bottom sheet opening/closing and keep the marker centered
            // TODO: Also perhaps make the marker animated to indicate clearly which marker the user is looking at
            bottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);

            // Return true to indicate that we handled the event
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
