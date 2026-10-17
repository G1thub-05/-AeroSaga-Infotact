package com.aerosaga.workflow;

import com.aerosaga.activity.DroneActivities;
import com.aerosaga.model.MissionRequest;
import com.aerosaga.model.MissionStatus;
import io.temporal.client.WorkflowOptions;
import io.temporal.testing.TestWorkflowEnvironment;
import io.temporal.worker.Worker;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

public class DroneMissionWorkflowTest {

    private TestWorkflowEnvironment testEnv;
    private Worker worker;
    private DroneMissionWorkflow workflow;
    private DroneActivities activities;

    @BeforeEach
    public void setUp() {
        testEnv = TestWorkflowEnvironment.newInstance();
        worker = testEnv.newWorker("DRONE_TASK_QUEUE");
        worker.registerWorkflowImplementationTypes(DroneMissionWorkflowImpl.class);

        // Mockito fix: withoutAnnotations() zaroori hai Temporal activities ke liye
        activities = mock(DroneActivities.class, withSettings().withoutAnnotations());
        
        // Mock behaviors
        when(activities.takeoff(anyString(), anyDouble(), anyDouble(), anyInt())).thenReturn("TAKEOFF_OK");
        when(activities.dropPackage(anyString(), anyBoolean())).thenReturn("DROP_OK");
        when(activities.land(anyString())).thenReturn("LAND_OK");
        // reportPosition void hai isliye usko mock return ki zaroorat nahi

        worker.registerActivitiesImplementations(activities);
        testEnv.start();

        workflow = testEnv.getWorkflowClient().newWorkflowStub(
                DroneMissionWorkflow.class,
                WorkflowOptions.newBuilder()
                        .setTaskQueue("DRONE_TASK_QUEUE")
                        .build()
        );
    }

    @AfterEach
    public void tearDown() {
        testEnv.close();
    }

    @Test
    public void testSuccessfulMission() {
        MissionRequest request = new MissionRequest();
        request.setDroneId("alpha");

        // Mission run karo
        MissionStatus status = workflow.runMission(request);

        // Verify state
        assertEquals("COMPLETED", status.getState());
        assertEquals("alpha", status.getDroneId());

        // Verify activities sahi se call hui hain
        verify(activities).takeoff(eq("alpha"), anyDouble(), anyDouble(), anyInt());
        verify(activities).dropPackage("alpha", false);
        verify(activities).land("alpha");
        
        // 20 times call hoga kyunki 10 steps jaane ke aur 10 aane ke
        verify(activities, times(20)).reportPosition(eq("alpha"), anyDouble(), anyDouble(), anyInt(), anyString());
    }
}