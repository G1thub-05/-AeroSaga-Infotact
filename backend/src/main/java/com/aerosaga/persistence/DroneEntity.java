package com.aerosaga.persistence;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import com.aerosaga.telemetry.Drone;

@Document(collection = "drones")
public class DroneEntity {
    @Id
    private String id;
    private String name;
    private String status;
    private double latitude;
    private double longitude;
    private int battery;
    private String missionId;
    private String currentStep;
    private boolean online;
    private String message;

    protected DroneEntity() {
    }

    public DroneEntity(Drone drone) {
        update(drone);
    }

    public void update(Drone drone) {
        id = drone.id();
        name = drone.name();
        status = drone.status();
        latitude = drone.latitude();
        longitude = drone.longitude();
        battery = drone.battery();
        missionId = drone.missionId();
        currentStep = drone.currentStep();
        online = drone.online();
        message = drone.message();
    }

    public Drone toDrone() {
        return new Drone(id, name, status, latitude, longitude, battery, missionId, currentStep, online, message);
    }
}
