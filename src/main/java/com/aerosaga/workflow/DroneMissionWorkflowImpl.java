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
        
        // Ahmedabad ki base location se start
        double startLat = 23.04;
        double startLon = 72.60;
        
        // Har drone ko map par alag direction mein bhejenge taaki overlapping na ho
        double targetLat = 23.05;
        double targetLon = 72.62;
        
        if (droneId.contains("1")) { targetLat = 23.08; targetLon = 72.58; }      // North-West
        else if (droneId.contains("2")) { targetLat = 23.01; targetLon = 72.64; } // South-East
        else if (droneId.contains("3")) { targetLat = 23.06; targetLon = 72.66; } // North-East
        else if (droneId.contains("4")) { targetLat = 23.00; targetLon = 72.57; } // South-West
        else if (droneId.contains("5")) { targetLat = 23.05; targetLon = 72.55; } // West

        int battery = 100;

        status.setDroneId(droneId);
        
        // 1. Takeoff
        status.setState("AIRBORNE");
        activities.takeoff(droneId, startLat, startLon, battery);
        Workflow.sleep(Duration.ofSeconds(1));

        // 2. Navigate to Target (Delivery dene jaa raha hai)
        status.setState("NAVIGATING");
        navigate(droneId, startLat, startLon, targetLat, targetLon, battery, "NAVIGATING");
        battery -= 10; // Battery thodi kam hui

        // 3. Drop Package
        status.setState("DROPPING_PACKAGE");
        activities.dropPackage(droneId, false);
        Workflow.sleep(Duration.ofSeconds(2)); // Drop karte time 2 second rukega

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
        int steps = 15; // 15 tukdon mein smooth movement karega
        for (int i = 1; i <= steps; i++) {
            double currentLat = fromLat + ((toLat - fromLat) * i / steps);
            double currentLon = fromLon + ((toLon - fromLon) * i / steps);
            
            int currentBattery = battery - (i / 2); // Battery dheere-dheere kam hogi
            
            // UI par position bhejo
            activities.reportPosition(droneId, currentLat, currentLon, currentBattery, stateName);
            
            // Temporal ka sleep (1.5 seconds rukega taaki aap map par live movement dekh sakein)
            Workflow.sleep(Duration.ofMillis(1500));
        }
    }
}