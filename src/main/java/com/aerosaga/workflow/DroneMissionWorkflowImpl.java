package com.aerosaga.workflow;

import com.aerosaga.activity.DroneActivities;
import com.aerosaga.model.MissionRequest;
import com.aerosaga.model.MissionStatus;
import io.temporal.activity.ActivityOptions;
import io.temporal.workflow.Workflow;
import java.time.Duration;

public class DroneMissionWorkflowImpl implements DroneMissionWorkflow {

    private final DroneActivities activities = Workflow.newActivityStub(
            DroneActivities.class,
            ActivityOptions.newBuilder()
                    .setStartToCloseTimeout(Duration.ofSeconds(15))
                    .build());

    private final MissionStatus status = new MissionStatus();

    @Override
    public MissionStatus runMission(MissionRequest request) {
        status.droneId = request.droneId;
        status.state = "RUNNING";
        status.currentStep = "TAKEOFF";

        activities.takeoff(request.droneId);

        status.state = "COMPLETED";
        status.message = "Takeoff done";
        return status;
    }
}