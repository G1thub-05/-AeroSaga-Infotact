package com.aerosaga.persistence;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import com.aerosaga.temporal.MissionRequest;
import com.aerosaga.temporal.MissionState;

@Document(collection = "missions")
public class MissionEntity {
    @Id
    private String missionId;
    private String droneId;
    private double startLat;
    private double startLon;
    private double destinationLat;
    private double destinationLon;
    private boolean simulateDropFailure;
    private String status;
    private String currentStep;
    private boolean online;
    private double latitude;
    private double longitude;
    private String message;
    private long updatedAt;

    protected MissionEntity() {
    }

    public MissionEntity(MissionRequest request) {
        missionId = request.missionId();
        droneId = request.droneId();
        startLat = request.startLat();
        startLon = request.startLon();
        destinationLat = request.destinationLat();
        destinationLon = request.destinationLon();
        simulateDropFailure = request.simulateDropFailure();
        status = "STARTED";
        currentStep = "QUEUED";
        online = true;
        latitude = request.startLat();
        longitude = request.startLon();
        message = "Mission accepted";
        updatedAt = System.currentTimeMillis();
    }

    public MissionState toState() {
        return new MissionState(missionId, droneId, status, currentStep, online, latitude, longitude, message,
                updatedAt);
    }

    public void update(MissionState state) {
        status = state.status();
        currentStep = state.currentStep();
        online = state.online();
        latitude = state.latitude();
        longitude = state.longitude();
        message = state.message();
        updatedAt = state.updatedAt();
    }
}
