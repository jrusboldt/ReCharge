package com.recharge;

public class StationMarkerData {

    public static final int STATUS_AVAILABLE = 0;
    public static final int STATUS_UNAVAILABLE = 1;
    public static final int STATUS_UNKNOWN = 2;

    private boolean isPaid;
    private boolean hasLevel1;
    private boolean hasLevel2;
    private boolean hasDC;

    private double distance;

    private int chargingAvailability;
    private int parkingAvailability;

    private String ID;
    private String image;
    private String publicStatus;
    private String workingStatus;
    private String address;
    private String expectedDate;

    public String getID() {
        return ID;
    }

    public void setID(String ID) {
        this.ID = ID;
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

    public double getDistance() {
        return distance;
    }

    public void setDistance(double distance) {
        this.distance = (double) Math.round(distance * 100) / 100;
    }

    public int getChargingAvailability() {
        return chargingAvailability;
    }

    public void setChargingAvailability(int availability) {
        this.chargingAvailability = availability;
    }

    public int getParkingAvailability() {
        return parkingAvailability;
    }

    public void setParkingAvailability(int availability) {
        this.parkingAvailability = availability;
    }

    public void setPublicStatus(String status) {
        this.publicStatus = status;
    }

    public String getPublicStatus() {
        return this.publicStatus;
    }

    public void setWorkingStatus(String status) {
        this.workingStatus = status;
    }

    public String getWorkingStatus() {
        return this.workingStatus;
    }

    public void setIsPaid(boolean isPaid) {
        this.isPaid = isPaid;
    }

    public boolean getIsPaid() {
        return this.isPaid;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getAddress() {
        return this.address;
    }

    public void setChargingLevels(boolean level1, boolean level2, boolean DC) {
        this.hasLevel1 = level1;
        this.hasLevel2 = level2;
        this.hasDC = DC;
    }

    public String getChargingLevels() {
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

    public void setExpectedDate(String expectedDate) {
        this.expectedDate = expectedDate;
    }

    public String getExpectedDate() {
        return this.expectedDate;
    }
}
