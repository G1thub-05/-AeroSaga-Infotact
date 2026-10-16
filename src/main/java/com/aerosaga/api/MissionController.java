package com.aerosaga.api;

import com.aerosaga.model.MissionRequest;
import com.aerosaga.model.MissionStatus;
import com.aerosaga.workflow.DroneMissionWorkflow;
import com.aerosaga.workflow.FleetMissionWorkflow;
import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowOptions;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class MissionController {

    private final WorkflowClient workflowClient;

    public MissionController(WorkflowClient workflowClient) {
        this.workflowClient = workflowClient;
    }

    @PostMapping("/missions")
    public String startMission(@RequestBody MissionRequest request) {
        String workflowId = "mission-" + request.getDroneId();
        WorkflowOptions options = WorkflowOptions.newBuilder()
                .setTaskQueue("DRONE_TASK_QUEUE")
                .setWorkflowId(workflowId)
                .build();

        DroneMissionWorkflow workflow = workflowClient.newWorkflowStub(DroneMissionWorkflow.class, options);
        
        // Asynchronous start (blocks nahi karega)
        WorkflowClient.start(workflow::runMission, request);
        
        return "Mission started for drone: " + request.getDroneId();
    }

    @GetMapping("/missions/{droneId}/state")
    public MissionStatus getMissionState(@PathVariable String droneId) {
        String workflowId = "mission-" + droneId;
        DroneMissionWorkflow workflow = workflowClient.newWorkflowStub(DroneMissionWorkflow.class, workflowId);
        
        return workflow.getStatus();
    }

    // NAYA ENDPOINT: Fleet start karne ke liye
    @PostMapping("/fleet/start")
    public String startFleet(@RequestParam(defaultValue = "5") int count) {
        WorkflowOptions options = WorkflowOptions.newBuilder()
                .setTaskQueue("DRONE_TASK_QUEUE")
                .setWorkflowId("fleet-mission-" + System.currentTimeMillis())
                .build();

        FleetMissionWorkflow workflow = workflowClient.newWorkflowStub(FleetMissionWorkflow.class, options);
        
        // Asynchronous start
        WorkflowClient.start(workflow::startFleet, count);
        
        return "Fleet mission started with " + count + " drones!";
    }
}