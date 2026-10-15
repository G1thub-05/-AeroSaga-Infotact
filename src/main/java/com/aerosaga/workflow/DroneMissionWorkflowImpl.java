package com.aerosaga.workflow;

import com.aerosaga.activity.DroneActivities;
import com.aerosaga.model.MissionRequest;
import com.aerosaga.model.MissionStatus;
import io.temporal.activity.ActivityOptions;
import io.temporal.workflow.Workflow;

import java.time.Duration;

public class DroneMissionWorkflowImpl implements DroneMissionWorkflow {

    // Activity options set kar rahe hain
    private final DroneActivities activities = Workflow.newActivityStub(DroneActivities.class,
            ActivityOptions.newBuilder()
                    .setStartToCloseTimeout(Duration.ofMinutes(5))
                    .build());

    private final MissionStatus status = new MissionStatus();

    @Override
    public MissionStatus runMission(MissionRequest request) {
        String droneId = request.getDroneId();
        
        // Ahmedabad ki location se start (Week 1 jaisa)
        double startLat = 23.04;
        double startLon = 72.60;
        // Delivery location thodi door set karte hain
        double targetLat = 23.05;
        double targetLon = 72.62;
        int battery = 100;

        status.setDroneId(droneId);
        
        // 1. Takeoff
        status.setState("AIRBORNE");
        activities.takeoff(droneId, startLat, startLon, battery);

        // 2. Navigate to Target (Delivery dene jaa raha hai)
        status.setState("NAVIGATING");
        navigate(droneId, startLat, startLon, targetLat, targetLon, battery, "NAVIGATING");
        battery -= 5; // Battery thodi kam hui

        // 3. Drop Package
        status.setState("DROPPING_PACKAGE");
        activities.dropPackage(droneId, false);

        // 4. Return to Base (Wapas aa raha hai)
        status.setState("RETURNING");
        navigate(droneId, targetLat, targetLon, startLat, startLon, battery, "RETURNING");

        // 5. Land
        status.setState("LANDING");
        activities.land(droneId);
        
        status.setState("COMPLETED");
        return status;
    }

    @Override
    public MissionStatus getStatus() {
        return status; // QueryMethod ke liye latest status return karega
    }

    // Yeh method drone ko step-by-step move karega
    private void navigate(String droneId, double fromLat, double fromLon, double toLat, double toLon, int battery, String stateName) {
        int steps = 10; // 10 tukdon mein rasta cover karega
        for (int i = 1; i <= steps; i++) {
            double currentLat = fromLat + ((toLat - fromLat) * i / steps);
            double currentLon = fromLon + ((toLon - fromLon) * i / steps);
            
            // UI par position bhejo
            activities.reportPosition(droneId, currentLat, currentLon, battery, stateName);
            
            // Temporal ka sleep (2 seconds rukega phir aage badhega)
            Workflow.sleep(Duration.ofSeconds(2));
        }
    }
}