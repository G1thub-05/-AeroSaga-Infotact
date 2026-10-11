package com.aerosaga.activity;

import io.temporal.activity.ActivityInterface;
import io.temporal.activity.ActivityMethod;

@ActivityInterface
public interface DroneActivities {

    @ActivityMethod
    String takeoff(String droneId);
}