package com.aerosaga.temporal;

import io.temporal.activity.ActivityInterface;
import io.temporal.activity.ActivityMethod;

@ActivityInterface
public interface MissionActivities {
    @ActivityMethod void report(String missionId, String droneId, String step, String status,
                                double latitude, double longitude, String message);
    @ActivityMethod void perform(String step, boolean failThisStep);
}
