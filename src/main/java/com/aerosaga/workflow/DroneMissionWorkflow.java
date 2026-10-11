package com.aerosaga.workflow;

import com.aerosaga.model.MissionRequest;
import com.aerosaga.model.MissionStatus;
import io.temporal.workflow.WorkflowInterface;
import io.temporal.workflow.WorkflowMethod;

@WorkflowInterface
public interface DroneMissionWorkflow {

    @WorkflowMethod
    MissionStatus runMission(MissionRequest request);
}