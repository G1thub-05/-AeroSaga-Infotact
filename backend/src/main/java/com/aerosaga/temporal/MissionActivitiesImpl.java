package com.aerosaga.temporal;

import com.aerosaga.telemetry.FleetService;
import io.temporal.activity.Activity;
import org.springframework.stereotype.Component;

@Component
public class MissionActivitiesImpl implements MissionActivities {
	private final FleetService fleet;
	public MissionActivitiesImpl(FleetService fleet) { this.fleet = fleet; }
	@Override public void report(String missionId, String droneId, String step, String status,
	                             double latitude, double longitude, String message) {
		fleet.updateMission(missionId, droneId, step, status, latitude, longitude, message);
	}
	@Override public void perform(String step, boolean failThisStep) {
		try { Thread.sleep("RETURN_TO_BASE".equals(step) ? 1800 : 2200); }
		catch (InterruptedException e) { Thread.currentThread().interrupt(); throw Activity.wrap(e); }
		if (failThisStep) throw new IllegalStateException("Simulated activity failure at " + step);
	}
}
