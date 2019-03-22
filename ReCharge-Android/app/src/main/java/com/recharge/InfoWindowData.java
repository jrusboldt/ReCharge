package com.recharge;

public class InfoWindowData {
    private String image;
    private double distance;
    private String chargingAvailability;
    private String parkingAvailability;

    private String publicStatus;

    public String getImage() {
        return image;
    }

    public void setImage(String image) {
        this.image = image;
    }

    public double getDistance() {
        return distance;
    }

    public void setDistance(double distance) {
        this.distance = distance;
    }

    public String getChargingAvailability() {
        return chargingAvailability;
    }

    public void setChargingAvailability(boolean availability) {
        if (availability) {
            chargingAvailability = "Available";
        } else {
            chargingAvailability = "Unknown";
        }
    }

    public String getParkingAvailability() {
        return parkingAvailability;
    }

    public void setParkingAvailability(boolean availability) {
        if (availability) {
            this.parkingAvailability = "Available";
        } else {
            this.parkingAvailability = "Unknown";
        }
    }

    public void setPublicStatus(String status) {
        this.publicStatus = status;
    }

    public String getPublicStatus() {
        return this.publicStatus;
    }
}
