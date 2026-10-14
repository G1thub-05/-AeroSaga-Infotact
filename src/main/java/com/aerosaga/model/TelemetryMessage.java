package com.aerosaga.model;

public class TelemetryMessage {
    private String droneId;
    private double latitude;
    private double longitude;
    private int battery;
    private String status;

    public TelemetryMessage() {
    }

    public TelemetryMessage(String droneId, double latitude, double longitude, int battery, String status) {
        this.droneId = droneId;
        this.latitude = latitude;
        this.longitude = longitude;
        this.battery = battery;
        this.status = status;
    }

    public String getDroneId() { return droneId; }
    public void setDroneId(String droneId) { this.droneId = droneId; }
    
    public double getLatitude() { return latitude; }
    public void setLatitude(double latitude) { this.latitude = latitude; }
    
    public double getLongitude() { return longitude; }
    public void setLongitude(double longitude) { this.longitude = longitude; }
    
    public int getBattery() { return battery; }
    public void setBattery(int battery) { this.battery = battery; }
    
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}