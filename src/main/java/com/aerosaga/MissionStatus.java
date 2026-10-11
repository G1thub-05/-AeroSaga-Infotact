package com.aerosaga.model;

import java.util.ArrayList;
import java.util.List;

public class MissionStatus {
    public String droneId;
    public String state = "PENDING";
    public String currentStep = "";
    public String message = "";
    public int battery = 100;
    public double lat;
    public double lon;
    public List<StepInfo> steps = new ArrayList<>();

    public MissionStatus() {
    }
}