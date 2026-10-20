package com.aerosaga.temporal;

import io.temporal.workflow.QueryMethod;
import io.temporal.workflow.SignalMethod;
import io.temporal.workflow.WorkflowInterface;
import io.temporal.workflow.WorkflowMethod;

@WorkflowInterface
public interface MissionWorkflow {
	@WorkflowMethod String runMission(MissionRequest request);
	@SignalMethod void setOnline(boolean online);
	@SignalMethod void emergencyAbort();
	@QueryMethod MissionState getState();
}
