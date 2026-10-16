package com.aerosaga.config;

import com.aerosaga.activity.DroneActivitiesImpl;
import com.aerosaga.workflow.DroneMissionWorkflowImpl;
import com.aerosaga.workflow.FleetMissionWorkflowImpl;
import io.temporal.client.WorkflowClient;
import io.temporal.serviceclient.WorkflowServiceStubs;
import io.temporal.worker.Worker;
import io.temporal.worker.WorkerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class TemporalConfig {

    private final DroneActivitiesImpl droneActivities;

    public TemporalConfig(DroneActivitiesImpl droneActivities) {
        this.droneActivities = droneActivities;
    }

    @Bean
    public WorkflowServiceStubs workflowServiceStubs() {
        return WorkflowServiceStubs.newLocalServiceStubs();
    }

    @Bean
    public WorkflowClient workflowClient(WorkflowServiceStubs workflowServiceStubs) {
        return WorkflowClient.newInstance(workflowServiceStubs);
    }

    @Bean
    public WorkerFactory workerFactory(WorkflowClient workflowClient) {
        WorkerFactory factory = WorkerFactory.newInstance(workflowClient);
        Worker worker = factory.newWorker("DRONE_TASK_QUEUE");

        // DONO workflows ko register kar rahe hain
        worker.registerWorkflowImplementationTypes(
                DroneMissionWorkflowImpl.class,
                FleetMissionWorkflowImpl.class
        );

        // Activities register kar rahe hain
        worker.registerActivitiesImplementations(droneActivities);

        factory.start();
        return factory;
    }
}