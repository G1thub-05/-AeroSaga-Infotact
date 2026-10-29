package com.aerosaga.temporal;

import com.aerosaga.telemetry.FleetService;
import io.temporal.activity.Activity;
import org.springframework.stereotype.Component;

/**
 * Implementation of MissionActivities.
 * Executes mission-related activities and reports mission updates
 * to the fleet management service.
 */
@Component
public class MissionActivitiesImpl implements MissionActivities {
	private final FleetService fleet;
	public MissionActivitiesImpl(FleetService fleet) { this.fleet = fleet; }

	/**
	 * Reports mission progress, status, and drone location.
	 * Delegates the update operation to FleetService.
	 * @param missionId unique identifier of the mission
	 * @param droneId   unique identifier of the drone
	 * @param step      mission step being reported
	 * @param status    current status of the mission step
	 * @param latitude  drone's latitude coordinate
	 * @param longitude drone's longitude coordinate
	 * @param message   additional details about the mission update
	 */
	@Override public void report(String missionId, String droneId, String step, String status,
	                             double latitude, double longitude, String message) {
		fleet.updateMission(missionId, droneId, step, status, latitude, longitude, message);
	}

	/**
	 * Executes a mission step.
	 * Simulates the time required to perform the step and optionally
	 * throws an exception to test Temporal's failure-handling behavior.
	 * @param step         name of the mission step to execute
	 * @param failThisStep true to simulate an activity failure; otherwise false
	 */
	@Override public void perform(String step, boolean failThisStep) {
		try { Thread.sleep("RETURN_TO_BASE".equals(step) ? 1800 : 2200); }
		catch (InterruptedException e) { Thread.currentThread().interrupt(); throw Activity.wrap(e); }

		// Simulate a failure when requested, for testing retries and error handling.
		if (failThisStep) throw new IllegalStateException("Simulated activity failure at " + step);
	}
}
