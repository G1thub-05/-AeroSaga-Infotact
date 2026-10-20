package com.aerosaga.temporal;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowClientOptions;
import io.temporal.serviceclient.WorkflowServiceStubs;
import io.temporal.serviceclient.WorkflowServiceStubsOptions;
import io.temporal.worker.Worker;
import io.temporal.worker.WorkerFactory;
import jakarta.annotation.PreDestroy;

@Configuration
@ConditionalOnProperty(prefix = "aerosaga.temporal", name = "enabled", havingValue = "true", matchIfMissing = true)
public class TemporalConfiguration {
	@Value("${aerosaga.temporal.target}")
	private String target;
	@Value("${aerosaga.temporal.namespace}")
	private String namespace;
	@Value("${aerosaga.temporal.task-queue}")
	private String taskQueue;
	private WorkerFactory factory;

	@Bean
	WorkflowServiceStubs workflowServiceStubs() {
		return WorkflowServiceStubs.newServiceStubs(WorkflowServiceStubsOptions.newBuilder().setTarget(target).build());
	}

	@Bean
	WorkflowClient workflowClient(WorkflowServiceStubs stubs) {
		return WorkflowClient.newInstance(stubs, WorkflowClientOptions.newBuilder().setNamespace(namespace).build());
	}

	@Bean
	WorkerFactory workerFactory(WorkflowClient client, MissionActivitiesImpl activities) {
		factory = WorkerFactory.newInstance(client);
		Worker worker = factory.newWorker(taskQueue);
		worker.registerWorkflowImplementationTypes(MissionWorkflowImpl.class);
		worker.registerActivitiesImplementations(activities);
		factory.start();
		return factory;
	}

	@PreDestroy
	void stopWorker() {
		if (factory != null)
			factory.shutdown();
	}
}
