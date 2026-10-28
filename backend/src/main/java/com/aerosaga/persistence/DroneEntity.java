package com.aerosaga.persistence;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import com.aerosaga.telemetry.Drone;


/**
 * Represents a drone document stored in the MongoDB database.
 * Maps the Drone telemetry model to a MongoDB document
 * and provides conversion between Drone and DroneEntity.
 */
@Document(collection = "drones")
public class DroneEntity {

	/**
	 * Unique identifier of the drone document.
	 * MongoDB uses this field as the document ID.
	 */
    @Id
    private String id;

    private String name;              // Name of the drone.
    private String status;            // Current operational status of the drone.
    private double latitude;          // Current latitude coordinate of the drone.
    private double longitude;         // Current longitude coordinate of the drone.
    private int battery;              // Current battery level of the drone.
    private String missionId;         // Identifier of the mission assigned to the drone.
    private String currentStep;       // Current execution step of the drone's mission.
    private boolean online;           // Indicates whether the drone is currently online.
    private String message;           // Message containing additional drone information or status details.

	/**
	 * Protected no-argument constructor.
	 * Allows Spring Data MongoDB to instantiate the entity
	 * when reading documents from the database.
	 */
    protected DroneEntity() {}

	/**
	 * Creates a DroneEntity from the given Drone telemetry model.
	 * @param drone the source drone containing telemetry data
	 */
    public DroneEntity(Drone drone) {
        update(drone);
    }


	/**
	 * Updates this entity's fields using the values
	 * from the provided Drone telemetry model.
	 * @param drone the drone containing the latest telemetry data
	 */
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

	/**
	 * Converts this persistence entity into a Drone telemetry model.
	 * @return a Drone object containing the entity's current data
	 */
    public Drone toDrone() {
        return new Drone(id, name, status, latitude, longitude, battery, missionId, currentStep, online, message);
    }
}
