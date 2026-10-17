package com.aerosaga.model;

public class MissionStatus {
    private String droneId;
    private String state;

    public MissionStatus() {
    }

    public MissionStatus(String droneId, String state) {
        this.droneId = droneId;
        this.state = state;
    }

    public String getDroneId() { return droneId; }
    public void setDroneId(String droneId) { this.droneId = droneId; }

    public String getState() { return state; }
    public void setState(String state) { this.state = state; }
}