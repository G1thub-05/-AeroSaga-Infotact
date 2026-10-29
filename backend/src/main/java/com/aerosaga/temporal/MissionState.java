package com.aerosaga.temporal;

/**
 * A record class that represents the current state of a drone mission.
 * A record is a concise data carrier that automatically generates a
 * canonical constructor, accessor methods, equals(), hashCode(), and toString().
 * Record components are final, meaning their values cannot be reassigned after the record object is created.
 *
 * @param missionId   unique identifier of the mission
 * @param droneId     unique identifier of the drone assigned to the mission
 * @param status      current overall mission status, such as IN_PROGRESS, COMPLETED, or FAILED
 * @param currentStep current step being executed in the mission
 * @param online      indicates whether the drone is currently online
 * @param latitude    latest reported latitude coordinate of the drone
 * @param longitude   latest reported longitude coordinate of the drone
 * @param message     additional information about the mission or its status
 * @param updatedAt   timestamp indicating when the mission state was last updated,typically represented as Unix epoch time in milliseconds
 */
public record MissionState(String missionId, String droneId, String status, String currentStep,
                           boolean online, double latitude, double longitude, String message,
                           long updatedAt) {}
