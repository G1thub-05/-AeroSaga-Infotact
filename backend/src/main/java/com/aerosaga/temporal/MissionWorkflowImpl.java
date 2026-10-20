package com.aerosaga.temporal;

import java.time.Duration;

import io.temporal.activity.ActivityOptions;
import io.temporal.common.RetryOptions;
import io.temporal.workflow.Workflow;

public class MissionWorkflowImpl implements MissionWorkflow {
	private static final int ROUTE_POINTS = 15;
	private static final Duration TELEMETRY_INTERVAL = Duration.ofSeconds(1);
	private final MissionActivities activities = Workflow.newActivityStub(MissionActivities.class,
			ActivityOptions.newBuilder().setStartToCloseTimeout(Duration.ofMinutes(2))
					.setRetryOptions(RetryOptions.newBuilder().setMaximumAttempts(3).build()).build());
	private boolean online = true;
	private boolean aborted;
	private MissionState state;

	@Override
	public String runMission(MissionRequest r) {
		update(r, "ACTIVE", "TAKEOFF", r.startLat(), r.startLon(), "Mission accepted");
		activities.perform("TAKEOFF", false);

		if (aborted)
			return abortAndReturn(r);
		update(r, "ACTIVE", "NAVIGATE", state.latitude(), state.longitude(), "En route to delivery destination");
		if (!move(r, r.destinationLat(), r.destinationLon(), "NAVIGATE", true))
			return abortAndReturn(r);

		update(r, "ACTIVE", "DROP_PACKAGE", r.destinationLat(), r.destinationLon(), "Dropping package");
		try {
			activities.perform("DROP_PACKAGE", r.simulateDropFailure());
		} catch (RuntimeException failure) {
			update(r, "RETURNING", "RETURN_TO_BASE", state.latitude(), state.longitude(),
					"Drop failed; compensating by returning to base");
			activities.perform("RETURN_TO_BASE", false);
			move(r, r.startLat(), r.startLon(), "RETURN_TO_BASE", false);
			update(r, "FAILED_SAFE", "RETURN_TO_BASE", r.startLat(), r.startLon(),
					"Mission failed; compensation returned drone to base");
			return "FAILED_SAFE";
		}

		if (aborted)
			return abortAndReturn(r);
		update(r, "RETURNING", "RETURN_TO_BASE", r.destinationLat(), r.destinationLon(),
				"Package delivered; returning to base");
		move(r, r.startLat(), r.startLon(), "RETURN_TO_BASE", false);
		update(r, "COMPLETED", "COMPLETED", r.startLat(), r.startLon(), "Package delivered; drone returned");
		return "COMPLETED";
	}

	/**
	 * Emits deterministic GPS waypoints once per second for responsive mission
	 * progress.
	 */
	private boolean move(MissionRequest r, double targetLat, double targetLon, String step, boolean stopOnAbort) {
		double fromLat = state.latitude(), fromLon = state.longitude();
		for (int point = 1; point <= ROUTE_POINTS; point++) {
			awaitConnection(r);
			if (stopOnAbort && aborted)
				return false;
			Workflow.sleep(TELEMETRY_INTERVAL);
			awaitConnection(r);
			double progress = (double) point / ROUTE_POINTS;
			update(r, "RETURN_TO_BASE".equals(step) ? "RETURNING" : "ACTIVE", step,
					fromLat + (targetLat - fromLat) * progress,
					fromLon + (targetLon - fromLon) * progress,
					"RETURN_TO_BASE".equals(step) ? "Returning to base" : "En route to delivery destination");
		}
		return true;
	}

	private String abortAndReturn(MissionRequest r) {
		update(r, "RETURNING", "RETURN_TO_BASE", state.latitude(), state.longitude(),
				"Abort received; returning to base");
		move(r, r.startLat(), r.startLon(), "RETURN_TO_BASE", false);
		update(r, "ABORTED", "RETURN_TO_BASE", r.startLat(), r.startLon(), "Drone safely returned to base after abort");
		return "ABORTED";
	}

	private void awaitConnection(MissionRequest r) {
		if (!online) {
			update(r, "PAUSED", state.currentStep(), state.latitude(), state.longitude(),
					"Telemetry lost; workflow is durably waiting for reconnect");
			Workflow.await(() -> online || aborted);
			if (online)
				update(r, "ACTIVE", state.currentStep(), state.latitude(), state.longitude(),
						"Drone reconnected; mission resumed");
		}
	}

	private void update(MissionRequest r, String status, String step, double lat, double lon, String message) {
		state = new MissionState(r.missionId(), r.droneId(), status, step, online, lat, lon, message,
				Workflow.currentTimeMillis());
		activities.report(r.missionId(), r.droneId(), step, status, lat, lon, message);
	}

	@Override
	public void setOnline(boolean value) {
		online = value;
	}

	@Override
	public void emergencyAbort() {
		aborted = true;
	}

	@Override
	public MissionState getState() {
		return state;
	}
}
