package com.aerosaga.temporal;

import io.temporal.activity.ActivityInterface;
import io.temporal.activity.ActivityMethod;

/**
 * Defines the activities that can be executed by a Temporal Workflow.
 * Activities contain the actual business logic performed during workflow execution.
 */
@ActivityInterface
public interface MissionActivities {

	/**
	 * Reports the current status of a mission step along with the drone's location.
	 * @param missionId unique identifier of the mission
	 * @param droneId   unique identifier of the drone performing the mission
	 * @param step      name of the mission step being executed
	 * @param status    current status of the step, such as STARTED, COMPLETED, or FAILED
	 * @param latitude  current latitude coordinate of the drone
	 * @param longitude current longitude coordinate of the drone
	 * @param message   additional information or details about the step's status
	 */
	@ActivityMethod void report(String missionId, String droneId, String step, String status,
	                            double latitude, double longitude, String message);

	/**
	 * Performs a specific mission step.
	 * @param step          name of the mission step to execute
	 * @param failThisStep  indicates whether this step should intentionally fail
	 * (useful for testing failure handling and retry behavior)
	 */
	@ActivityMethod void perform(String step, boolean failThisStep);
}
