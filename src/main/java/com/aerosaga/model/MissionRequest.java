package com.aerosaga.model;

public class MissionRequest {
    public String droneId = "drone-1";
    public double baseLat = 23.0225;
    public double baseLon = 72.5714;
    public double destLat = 23.0625;
    public double destLon = 72.6114;
    public int navigateSeconds = 20;
    public boolean simulateDropFailure = false;
    public boolean simulateLowBattery = false;

    public MissionRequest() {
    }
}