package com.aerosaga.temporal;

import io.temporal.workflow.QueryMethod;
import io.temporal.workflow.SignalMethod;
import io.temporal.workflow.WorkflowInterface;
import io.temporal.workflow.WorkflowMethod;


/**
 * Defines the Temporal Workflow contract for managing a drone mission.
 * A workflow coordinates mission execution, handles external signals, and exposes the current mission state through a query method.
 */
@WorkflowInterface
public interface MissionWorkflow {

	/**
	 * Executes the drone mission using the provided mission request.
	 * This is the main workflow entry point. Temporal uses this method to start the workflow execution and obtain its final result.
	 * @param request mission details, including drone ID, coordinates, and failure-simulation configuration
	 * @return a string describing the final mission result
	 */
	@WorkflowMethod String runMission(MissionRequest request);

	/**
	 * Updates the drone's online/offline status during workflow execution.
	 * A signal allows an external caller to communicate with a running workflow without waiting for a return value.
	 * @param online true if the drone is online; false if it is offline
	 */
	@SignalMethod void setOnline(boolean online);

	/**
	 * Requests an emergency abort of the current mission.
	 * An external caller can send this signal to notify the workflow that the mission should stop according to its abort-handling logic.
	 */
	@SignalMethod void emergencyAbort();

	/**
	 * Retrieves the current state of the mission without modifying it.
	 * A query allows an external caller to inspect workflow state, such as mission status, current step, and drone location.
	 * @return the current mission state
	 */
	@QueryMethod MissionState getState();
}
