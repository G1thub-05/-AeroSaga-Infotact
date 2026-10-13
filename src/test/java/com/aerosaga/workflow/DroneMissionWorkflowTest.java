package com.aerosaga.workflow;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.withSettings;
import static org.mockito.Mockito.when;

import com.aerosaga.activity.DroneActivities;
import com.aerosaga.config.TemporalConfig;
import com.aerosaga.model.MissionRequest;
import com.aerosaga.model.MissionStatus;
import com.aerosaga.workflow.DroneMissionWorkflow;

import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowOptions;
import io.temporal.testing.TestWorkflowEnvironment;
import io.temporal.worker.Worker;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class DroneMissionWorkflowTest {

    private TestWorkflowEnvironment env;
    private Worker worker;
    private WorkflowClient client;

    @BeforeEach
    void setUp() {
        env = TestWorkflowEnvironment.newInstance();
        worker = env.newWorker(TemporalConfig.TASK_QUEUE);
        worker.registerWorkflowImplementationTypes(DroneMissionWorkflowImpl.class);
        client = env.getWorkflowClient();
    }

    @AfterEach
    void tearDown() {
        env.close();
    }

    @Test
    void takeoffMissionCompletes() {
        DroneActivities activities =
                mock(DroneActivities.class, withSettings().withoutAnnotations());
        when(activities.takeoff("test-drone")).thenReturn("TAKEOFF_OK");
        worker.registerActivitiesImplementations(activities);
        env.start();

        DroneMissionWorkflow workflow = client.newWorkflowStub(
                DroneMissionWorkflow.class,
                WorkflowOptions.newBuilder().setTaskQueue(TemporalConfig.TASK_QUEUE).build());
        MissionRequest req = new MissionRequest();
        req.droneId = "test-drone";

        MissionStatus result = workflow.runMission(req);

        assertEquals("COMPLETED", result.state);
        verify(activities).takeoff("test-drone");
    }
}