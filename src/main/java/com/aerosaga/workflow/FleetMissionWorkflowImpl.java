package com.aerosaga.workflow;

import com.aerosaga.model.MissionRequest;
import com.aerosaga.model.MissionStatus;
import io.temporal.workflow.Async;
import io.temporal.workflow.ChildWorkflowOptions;
import io.temporal.workflow.Promise;
import io.temporal.workflow.Workflow;

import java.util.ArrayList;
import java.util.List;

public class FleetMissionWorkflowImpl implements FleetMissionWorkflow {

    @Override
    public String startFleet(int numberOfDrones) {
        List<Promise<MissionStatus>> promises = new ArrayList<>();

        for (int i = 1; i <= numberOfDrones; i++) {
            String droneId = "fleet-drone-" + i;
            
            ChildWorkflowOptions options = ChildWorkflowOptions.newBuilder()
                    .setWorkflowId("mission-" + droneId) // Same ID pattern taaki status query chal sake
                    .build();

            DroneMissionWorkflow child = Workflow.newChildWorkflowStub(DroneMissionWorkflow.class, options);
            
            MissionRequest request = new MissionRequest();
            request.setDroneId(droneId);

            // Child workflow ko asynchronously start karo
            Promise<MissionStatus> promise = Async.function(child::runMission, request);
            promises.add(promise);
        }

        // Promise.allOf sabhi drones ke mission complete hone ka wait karega
        Promise.allOf(promises).get();

        return numberOfDrones + " drones fleet mission completed!";
    }
}