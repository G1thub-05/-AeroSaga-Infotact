package com.aerosaga.activity;

import io.temporal.activity.ActivityInterface;
import io.temporal.activity.ActivityMethod;

@ActivityInterface
public interface DroneActivities {

    @ActivityMethod
    String takeoff(String droneId, double lat, double lon, int battery);

    @ActivityMethod
    void reportPosition(String droneId, double lat, double lon, int battery, String status);

    @ActivityMethod
    String dropPackage(String droneId, boolean forceFail);

    @ActivityMethod
    String land(String droneId);
}