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


// Configures Temporal components required to connect to the Temporal server and execute workflows and activities.
@Configuration

// Creates this configuration only when Temporal is enabled.
// If the property is missing, Temporal is enabled by default.
@ConditionalOnProperty(prefix = "aerosaga.temporal", name = "enabled", havingValue = "true", matchIfMissing = true)
public class TemporalConfiguration {

	// Temporal server address, for example, localhost:7233.
	@Value("${aerosaga.temporal.target}")
	private String target;

	// Namespace in which workflows will execute.
	@Value("${aerosaga.temporal.namespace}")
	private String namespace;

	// Task queue used by workers to receive workflow and activity tasks.
	@Value("${aerosaga.temporal.task-queue}")
	private String taskQueue;

	// Stores the WorkerFactory reference so it can be shut down during Spring application termination.
	private WorkerFactory factory;


	/**
	 * Creates the connection configuration for the Temporal service.
	 * @return WorkflowServiceStubs used to communicate with Temporal Server
	 */
	@Bean
	WorkflowServiceStubs workflowServiceStubs() {

		// Configures the Temporal server target and creates service stubs.
		// Service stubs handle communication with the Temporal service.
		return WorkflowServiceStubs.newServiceStubs(WorkflowServiceStubsOptions.newBuilder().setTarget(target).build());
	}

	/**
	 * Creates a Temporal WorkflowClient.
	 * @param stubs connection interface for the Temporal service
	 * @return WorkflowClient configured for the specified namespace
	 */
	@Bean
	WorkflowClient workflowClient(WorkflowServiceStubs stubs) {

		// Creates the client used to interact with Temporal,
		// including starting workflows and accessing workflow information.
		return WorkflowClient.newInstance(stubs, WorkflowClientOptions.newBuilder().setNamespace(namespace).build());
	}

	/**
	 * Creates and starts a worker that processes tasks from the task queue.
	 * @param client Temporal client used by the worker factory
	 * @param activities implementation containing activity methods
	 * @return started WorkerFactory
	 */
	@Bean
	WorkerFactory workerFactory(WorkflowClient client, MissionActivitiesImpl activities) {

		// Creates a factory responsible for managing Temporal workers.
		factory = WorkerFactory.newInstance(client);

		// Creates a worker that polls the configured task queue for workflow and activity tasks.
		Worker worker = factory.newWorker(taskQueue);

		// Registers the workflow implementation that this worker can execute.
		worker.registerWorkflowImplementationTypes(MissionWorkflowImpl.class);

		// Registers the activity implementation so the worker can execute the activity methods called by workflows.
		worker.registerActivitiesImplementations(activities);

		// Starts the worker factory and begins polling for tasks.
		factory.start();
		return factory;
	}

	/**
	 * Shuts down the worker factory when the Spring application is stopped.
	 * This helps release worker resources and stop background processing.
	 */
	@PreDestroy
	void stopWorker() {

		// Checks whether the factory was initialized before shutting it down.
		if (factory != null)
			factory.shutdown();
	}
}
