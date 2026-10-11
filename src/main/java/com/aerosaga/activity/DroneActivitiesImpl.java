package com.aerosaga.activity;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class DroneActivitiesImpl implements DroneActivities {

    private static final Logger log = LoggerFactory.getLogger(DroneActivitiesImpl.class);

    @Override
    public String takeoff(String droneId) {
        log.info("Drone {} taking off...", droneId);
        try {
            Thread.sleep(1500);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        log.info("Drone {} is airborne", droneId);
        return "TAKEOFF_OK";
    }
}