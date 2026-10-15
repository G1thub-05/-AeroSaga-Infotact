package com.aerosaga.activity;

import com.aerosaga.model.TelemetryMessage;
import com.aerosaga.websocket.TelemetryBroadcaster;
import org.springframework.stereotype.Component;

@Component
public class DroneActivitiesImpl implements DroneActivities {

    private final TelemetryBroadcaster broadcaster;

    // Constructor se WebSocket broadcaster inject kar rahe hain
    public DroneActivitiesImpl(TelemetryBroadcaster broadcaster) {
        this.broadcaster = broadcaster;
    }

    @Override
    public String takeoff(String droneId, double lat, double lon, int battery) {
        System.out.println("Drone " + droneId + " taking off...");
        sleep(1500);
        broadcaster.broadcast(new TelemetryMessage(droneId, lat, lon, battery, "AIRBORNE"));
        return "TAKEOFF_OK";
    }

    @Override
    public void reportPosition(String droneId, double lat, double lon, int battery, String status) {
        // Yeh line map par drone ko move karegi!
        broadcaster.broadcast(new TelemetryMessage(droneId, lat, lon, battery, status));
    }

    @Override
    public String dropPackage(String droneId, boolean forceFail) {
        System.out.println("Drone " + droneId + " dropping package...");
        sleep(2000);
        if (forceFail) {
            throw new RuntimeException("Package drop failed!"); // Week 3 ki saga compensation ke liye
        }
        return "DROP_OK";
    }

    @Override
    public String land(String droneId) {
        System.out.println("Drone " + droneId + " landing...");
        sleep(1500);
        return "LAND_OK";
    }

    // Helper method delay ke liye
    private void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}