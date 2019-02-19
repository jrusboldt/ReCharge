package com.recharge;

import android.content.Intent;
import android.net.Uri;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback {

    // Get a list of the charging stations
    // (Right now, this is the nrel.txt file from the iOS app. This will be switched out with a dynamic list later)
    String stations[] = {"ELEC,Purdue University - Northwestern Parking Garage,460 Northwestern Ave,Located on " +
            "the ground level after entering from the northern entrance off of Northwestern Ave,West Lafayette,IN,47907,,765-494-9494  855-443-3873,E,,Public,24 hours daily,,,,,,1,,,,,GPS,40.4296753,-86.9120266,2018-07-09,46846,0.14342,2019-01-17 00:33:46 UTC,P,,,2012-02-15,,,,,J1772,US,0.23081,,,,Public,,public,,\n",
            "ELEC,Purdue University - Armory,812 3rd St,Located in the SE corner of the Armory,West Lafayette,IN," +
                    "47907,,765-494-9494  855-443-3873,E,,Public,24 hours daily,,,,,,1,,,,,GPS,40.4277617,-86.9162607,2018-07-09,46844,0.16141,2019-01-17 00:33:46 UTC,P,,,2012-02-15,,,,,J1772,US,0.25976,,,,Public,,public,,\n",
            "ELEC,Purdue University - University Street Garage,610 Purdue Mall,Located in the northwest corner of" +
                    " the garage entering off Waldron St,West Lafayette,IN,47907,,765-494-9494,E,,Public,24 hours daily,,,,,,1,,,,,GPS,40.426713,-86.917213,2018-07-09,76232,0.2245,2019-01-17 00:33:46 UTC,P,,,2016-01-01,,,,,J1772,US,0.3613,,,,Public,,public,,\n",
            "ELEC,Purdue University - Grant Street Parking Garage,120 N Grant St,Located at the southwest corner " +
                    "of the garage next to the lobby,West Lafayette,IN,47906,,765-494-9494  855-443-3873,E,,Public,24 hours daily,,,,,,1,,,,,GPS,40.4244203,-86.9103211,2018-07-09,46845,0.27825,2019-01-17 00:33:46 UTC,P,,,2012-02-15,,,,,J1772,US,0.4478,,,,Public,,public,,\n",
            "ELEC,Purdue University - Harrison Street Garage,719 Clinic Dr,Located on the first level on the west" +
                    " side of the garage,West Lafayette,IN,47907,,765-494-9494,E,,Public,24 hours daily,,,,,,1,,,,,GPS,40.421241,-86.917619,2018-07-09,76233,0.50903,2019-01-17 00:33:46 UTC,P,,,2016-01-01,,,,,J1772,US,0.8192,,,,Public,,public,,\n",
            "CNG,TruStar Energy - City Bus,1250 Canal Rd,,Lafayette,IN,47904,,,E,,Private,,,,Q,3600,,,,,,,200-9," +
                    "40.42923,-86.892351,2018-11-08,63626,1.10375,2019-01-17 00:33:46 UTC,LG,,,2015-04-09,,HD,,,,US,1.77631,,,,Privé,,private,,\n",
            "E85,Family Express - Purdue,1812 Northwestern Ave,,West Lafayette,IN,47906,,765-463-9469,E,,Public," +
                    "5am-11pm daily,A Cash D M V Voyager Wright_Exp,,,,,,,,,,200-9,40.445156,-86.920418,2018-07-09,71615,1.25656,2019-01-17 00:33:46 UTC,P,,,2015-12-15,,,,false,,US,2.02224,,,,Public,,public,,\n",
            "LPG,U-Haul,1090 Sagamore Pkwy W,,West Lafayette,IN,47906,,765-463-9502,E,,Public,7am-7pm M-Th and " +
                    "Sat 7am-8pm F 9am-5pm Sun,A Cash D M V,,,,,,,,,,200-9,40.454655,-86.919884,2018-10-04,68055,1.88617,2019-01-17 00:33:46 UTC,P,,,,,,true,,,US,3.0355,,,,Public,,public,,\n",
            "E85,Family Express - Lafayette,323 Sagamore Pkwy,,Lafayette,IN,47904,,765-446-9649,E,,Public," +
                    "5am-11pm daily,A Cash D M V Voyager Wright_Exp,,,,,,,,,,200-9,40.420143,-86.857666,2018-07-09,31672,2.97568,2019-01-17 00:33:46 UTC,P,,,2007-10-15,,,,false,,US,4.78889,,,,Public,,public,,\n",
            "ELEC,BILL DEFOUW BMW,178 Sagamore Pkwy S,PUBLIC 01; North side of parking lot PUBLIC 02; South side " +
                    "of building by flag pole STATION 01; -,Lafayette,IN,47905,,888-758-4389,E,,Public,24 hours daily,,,,,,6,,,ChargePoint Network,http://www.chargepoint.com/,200-8,40.415705,-86.858286,2019-02-10,92988,3.01366,2019-02-10 10:04:55 UTC,,,,,,,,,J1772,US,4.85002,,,,Public,,public,,\n",
            "ELEC,Duke Energy,3395 Greenbush St,,Lafayette,IN,47905,,,E,,Private,Employee and fleet vehicle use " +
                    "only,,,,,,8,,,,,200-9,40.428145,-86.85469,2019-01-10,86180,3.08524,2019-01-17 00:33:46 UTC,T,,,2015-02-20,,,,,J1772,US,4.96521,,,,Privé,,private,,\n",
            "E85,Meijer Gas #186,2740 US 52,,West Lafayette,IN,47906,,765-637-4129,E,,Public,24 hours daily,A " +
                    "Cash D M V Voyager Wright_Exp,,,,,,,,,,200-9,40.46975,-86.96015,2018-05-03,61144,3.80829,2019-01-17 00:33:46 UTC,P,,,2014-09-18,,,,false,,US,6.12885,,,,Public,,public,,\n",
            "ELEC,Nissan of Lafayette,1 N Creasy Ln,,Lafayette,IN,47905,,765-447-7575,E,,Public - Call ahead," +
                    "Dealership business hours,,,,,,1,,,,,200-9,40.418149,-86.837951,2018-07-09,47155,4.02355,2019-01-17 00:33:46 UTC,P,,,2012-01-31,,,,,J1772,US,6.47528,,,,Public - Appeler à l'avance,,public,CALL,\n",
            "ELEC,Nissan of Lafayette,1 N Creasy Ln,,Lafayette,IN,47905,,765-447-7575,E,,Private,,,,,,,1,,,,," +
                    "200-9,40.418149,-86.837951,2018-07-09,47156,4.02355,2019-01-17 00:33:46 UTC,P,,,2012-01-31,,,,,J1772,US,6.47528,,,,Privé,,private,,\n",
            "ELEC,Residence Inn - Lafayette - Tesla Destination,3834 Grace Ln,,Lafayette,IN,47906,,765-479-7208  " +
                    "877-798-3752,E,,Public,24 hours daily; for Tesla use only; for guest use only; see front desk for access,,,,,,4,,,Tesla Destination,https://www.tesla.com/destination-charging,200-8,40.418741,-86.834402,2019-01-10,114236,4.20194,2019-01-17 00:33:46 UTC,P,,,2018-11-01,,,,,TESLA,US,6.76237,,,,Public,,public,,\n",
            "E85,Family Express - Lafayette State Road 25,3015 State Rd 25 N,,Lafayette,IN,47905,,765-742-1420,E," +
                    ",Public,5am-12am daily,A Cash D M V Voyager Wright_Exp,,,,,,,,,,200-9,40.462265,-86.840401,2018-07-09,30934,4.51469,2019-01-17 00:33:46 UTC,P,,,2006-11-15,,,,true,,US,7.26569,,,,Public,,public,,\n",
            "E85,Speedway #3310,3875 State Rd 38 E,,Lafayette,IN,47905,,765-448-9018,E,,Public,24 hours daily,A " +
                    "Cash D M V Voyager Wright_Exp,,,,,,,,,,200-9,40.388986,-86.839789,2018-11-08,40352,4.70792,2019-01-17 00:33:46 UTC,P,,,2011-06-03,,,,false,,US,7.57666,,,,Public,,public,,\n",
            "ELEC,DoubleTree - Lafayette East - Tesla Destination,155 Progress Dr,,Lafayette,IN,47905,," +
                    "765-446-0900  877-798-3752,E,,Public,24 hours daily; for Tesla use only; for guest use only; see front desk for access,,,,,,4,,,Tesla Destination,https://www.tesla.com/destination-charging,200-8,40.416335,-86.824716,2019-01-10,114235,4.73262,2019-01-17 00:33:46 UTC,P,,,2018-11-01,,,,,TESLA,US,7.61641,,,,Public,,public,,"};
    // Default location for map
    // Right now, this location is the Purdue University Engineering Fountain
    double defaultLocLat = 40.4286;
    double defaultLocLng = -86.9138;

    private GoogleMap mMap;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
    }


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

        LatLng defaultLoc = new LatLng(defaultLocLat, defaultLocLng);
        int defaultZoom = 15;
        mMap.moveCamera(CameraUpdateFactory.newLatLng(defaultLoc));
        mMap.moveCamera(CameraUpdateFactory.zoomTo(defaultZoom));

        // Add a marker to the map for each station by getting the coordinates and name of each station
        for (String s : stations) {
            String[] station = s.split(",");
            LatLng stationLoc = new LatLng(Double.parseDouble(station[24]), Double.parseDouble(station[25]));
            mMap.addMarker(new MarkerOptions().position(stationLoc).title(station[1]));
        }
    }

    /*---------------------------------------------------------
     *  getPathToStation - Launches Google Maps navigation
     *      with turn-by-turn directions to the desired
     *      charging station from the user's current location.
     *---------------------------------------------------------
     */
    public void getPathToStation(LatLng stationLoc)
    {
        Intent googleMapIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("google.navigation:q="
                + stationLoc.latitude + "," + stationLoc.longitude));
        googleMapIntent.setPackage("com.google.android.apps.maps");

        /* Prevents crashing if Google Maps isn't installed */
        if (googleMapIntent.resolveActivity(getPackageManager()) != null)
            startActivity(googleMapIntent); /* Launch Google Maps */
    }
}
