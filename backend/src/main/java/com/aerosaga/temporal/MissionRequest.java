package com.aerosaga.temporal;

//A record class in Java is a special type of class used to store data with less boilerplate code.
// In MissionRequest, the record declaration automatically generates the
// constructor, accessor methods, equals(), hashCode(), and toString().


/**
 * Represents the input data required to execute a drone mission.
 * @param missionId            unique identifier of the mission
 * @param droneId              unique identifier of the drone assigned to the mission
 * @param startLat              starting latitude coordinate of the drone
 * @param startLon              starting longitude coordinate of the drone
 * @param destinationLat        destination latitude coordinate
 * @param destinationLon        destination longitude coordinate
 * @param simulateDropFailure  indicates whether to simulate a delivery/drop failure
 * for testing failure handling and retry behavior
 */
public record MissionRequest(String missionId, String droneId, double startLat, double startLon,
                             double destinationLat, double destinationLon, boolean simulateDropFailure) {}
