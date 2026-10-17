package com.aerosaga.model;

public class MissionRequest {
    private String droneId;

    public MissionRequest() {
    }

    public MissionRequest(String droneId) {
        this.droneId = droneId;
    }

    public String getDroneId() {
        return droneId;
    }

    public void setDroneId(String droneId) {
        this.droneId = droneId;
    }
}