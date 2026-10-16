package com.aerosaga.workflow;

import io.temporal.workflow.WorkflowInterface;
import io.temporal.workflow.WorkflowMethod;

@WorkflowInterface
public interface FleetMissionWorkflow {

    @WorkflowMethod
    String startFleet(int numberOfDrones);
}