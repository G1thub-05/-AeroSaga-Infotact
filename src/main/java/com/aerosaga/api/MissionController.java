package com.aerosaga.api;

import com.aerosaga.config.TemporalConfig;
import com.aerosaga.model.MissionRequest;
import com.aerosaga.workflow.DroneMissionWorkflow;
import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowOptions;
import java.util.Map;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class MissionController {

    private final WorkflowClient client;

    public MissionController(WorkflowClient client) {
        this.client = client;
    }

    @PostMapping("/missions")
    public Map<String, String> startMission(@RequestBody(required = false) MissionRequest request) {
        MissionRequest req = request != null ? request : new MissionRequest();
        String workflowId = "mission-" + req.droneId;

        DroneMissionWorkflow workflow = client.newWorkflowStub(
                DroneMissionWorkflow.class,
                WorkflowOptions.newBuilder()
                        .setTaskQueue(TemporalConfig.TASK_QUEUE)
                        .setWorkflowId(workflowId)
                        .build());

        WorkflowClient.start(workflow::runMission, req);
        return Map.of("workflowId", workflowId, "message", "Mission started");
    }
}