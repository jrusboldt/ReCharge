package com.recharge;

import android.content.Context;
import android.location.Location;

import com.google.android.gms.maps.model.LatLng;

import org.json.JSONException;
import org.json.JSONObject;

public class StationMarkerData {

    public static final int STATUS_AVAILABLE = 0;
    public static final int STATUS_UNAVAILABLE = 1;
    public static final int STATUS_UNKNOWN = 2;

    private Context context;

    private boolean isPaid;
    private boolean hasLevel1;
    private boolean hasLevel2;
    private boolean hasDC;

    private int chargingAvailability;
    private int parkingAvailability;

    private String ID;
    private String name;
    private String image;
    private String publicStatus;
    private String workingStatus;
    private String address;
    private String expectedDate;
    private String locationDetails;

    private LatLng location;

    public StationMarkerData(JSONObject station, Context context) throws JSONException {
        // Test
        this.context = context;

        // Save the station ID
        this.setID(station.getString("id"));

        // Save the station name
        this.setName(station.getString("station_name"));

        // Save the station address
        this.setAddress(station.getString("street_address") + ", " +
                station.getString("city") + ", " + station.getString("state"));

        // Save the station location
        this.setLocation(new LatLng(station.getDouble("latitude"), station.getDouble("longitude")));

        // Save the location details
        this.setLocationDetails(station.getString("intersection_directions"));

        // Save the working status
        this.setWorkingStatus(station.getString("status_code"));

        // Save the expected working date
        this.setExpectedDate(station.getString("expected_date"));

        // Save the paid status
        this.setIsPaid(!station.getString("cards_accepted").equals("null"));

        // Save the public access status
        this.setPublicStatus(station.getString("groups_with_access_code"));

        // Save the charging levels
        this.setChargingLevels(!station.getString("ev_level1_evse_num").equals("null"),
                !station.getString("ev_level2_evse_num").equals("null"),
                !station.getString("ev_dc_fast_num").equals("null"));

        // Save the charging availability status
        // Set this to "Unknown" for now as this will be updated dynamically later
        this.setChargingAvailability(StationMarkerData.STATUS_UNKNOWN);

        // Save the parking availability status
        // Set this to "Unknown" for now as this will be updated dynamically later
        this.setParkingAvailability(StationMarkerData.STATUS_UNKNOWN);

        // Save a default image for the station
        this.setImage("@drawable/ic_ev_station_black_24dp");
    }

    public String getID() {
        return this.ID;
    }

    public void setID(String ID) {
        this.ID = ID;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getAddress() {
        return this.address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public LatLng getLocation() {
        return location;
    }

    public void setLocation(LatLng location) {
        this.location = location;
    }

    public double getDistance(LatLng userLocation) {
        // Get the distance from the user to the station
        float[] distance = new float[2];
        Location.distanceBetween(userLocation.latitude,
                userLocation.longitude, location.latitude, location.longitude, distance);

        // Return the distance to the user
        // This distance is in meters, so it should be converted to miles
        return (distance[0] * 0.000621371);
    }

    public String getLocationDetails() {
        return this.locationDetails;
    }

    public void setLocationDetails(String locationDetails) {
        // If there are no details available, rewrite them to be more clear to the user
        if (locationDetails.equals("null")) {
            locationDetails = "Details unavailable";
        }

        this.locationDetails = locationDetails;
    }

    public String getWorkingStatus() {
        return this.workingStatus;
    }

    public String getWorkingStatusText() {
        String res = getWorkingStatus();

        // The status will be "E" if the station is Open
        // The status will be "P" if the station is Planned to be opened
        // The status will be "T" if the station is Temporarily Unavailable
        if (res.equals("E")) {
            return context.getString(R.string.station_status_operation_normal);
        } else if (res.equals("P")) {
            return context.getString(R.string.station_status_operation_planned);
        } else {
            return context.getString(R.string.station_status_operation_temp_unavailable);
        }
    }

    public void setWorkingStatus(String status) {
        this.workingStatus = status;

        // Special case for demo purposes
        // This sets the armory charging station to temporarily unavailable
        if (getID().equals("46844")) {
            this.workingStatus = "T";
        }

        // If the working status is not equal to "E", then it is unavailable for charging
        if (!status.equals("E")) {
            setChargingAvailability(StationMarkerData.STATUS_UNAVAILABLE);
        }
    }

    public String getExpectedDate() {
        return this.expectedDate;
    }

    public void setExpectedDate(String expectedDate) {
        // Rewrite the expected date as "None" if there was not one given
        if (expectedDate.equals("null")) {
            this.expectedDate = "None at this time";
        } else {
            this.expectedDate = expectedDate;
        }
    }

    public boolean getIsPaid() {
        return this.isPaid;
    }

    public String getIsPaidText() {
        if (getIsPaid()) {
            return context.getString(R.string.station_status_paid);
        } else {
            return context.getString(R.string.station_status_free);
        }
    }

    public void setIsPaid(boolean isPaid) {
        this.isPaid = isPaid;
    }

    public String getPublicStatus() {
        return this.publicStatus;
    }

    public void setPublicStatus(String status) {
        this.publicStatus = status;
    }

    public String getAllChargingLevels() {
        StringBuilder sb = new StringBuilder();
        if (this.hasLevel1) {
            if (sb.length() == 0) {
                sb.append("Level 1 EVSE");
            } else {
                sb.append(", Level 1 EVSE");
            }
        }

        if (this.hasLevel2) {
            if (sb.length() == 0) {
                sb.append("Level 2 EVSE");
            } else {
                sb.append(", Level 2 EVSE");
            }
        }

        if (this.hasDC) {
            if (sb.length() == 0) {
                sb.append("DC Fast");
            } else {
                sb.append(", DC Fast");
            }
        }

        return sb.toString();
    }

    public boolean hasLevel() {
        return hasLevel1;
    }

    public boolean hasLeve2() {
        return hasLevel2;
    }

    public boolean hasDC() {
        return hasDC;
    }

    public void setChargingLevels(boolean level1, boolean level2, boolean DC) {
        this.hasLevel1 = level1;
        this.hasLevel2 = level2;
        this.hasDC = DC;
    }

    public int getChargingAvailability() {
        return chargingAvailability;
    }

    public String getChargingAvailabilityText() {
        int res = getChargingAvailability();

        if (res == STATUS_AVAILABLE) {
            return context.getString(R.string.generic_yes);
        } else if (res == STATUS_UNAVAILABLE) {
            return context.getString(R.string.generic_no);
        } else {
            return context.getString(R.string.generic_unknown);
        }
    }

    public void setChargingAvailability(int availability) {
        this.chargingAvailability = availability;
    }

    public int getParkingAvailability() {
        return parkingAvailability;
    }

    public String getParkingAvailabilityText() {
        int res = getParkingAvailability();

        if (res == STATUS_AVAILABLE) {
            return context.getString(R.string.generic_yes);
        } else if (res == STATUS_UNAVAILABLE) {
            return context.getString(R.string.generic_no);
        } else {
            return context.getString(R.string.generic_unknown);
        }
    }

    public void setParkingAvailability(int availability) {
        this.parkingAvailability = availability;
    }

    public String getImage() {
        return image;
    }

    public void setImage(String image) {
        /*
        int imageId = context.getResources().getIdentifier(stationMarkerData.getImage().toLowerCase(),
                "drawable", context.getPackageName());
        img.setImageResource(imageId);
         */

        this.image = image;
    }
}
