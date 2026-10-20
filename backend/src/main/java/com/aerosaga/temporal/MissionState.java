package com.aerosaga.temporal;

public record MissionState(String missionId, String droneId, String status, String currentStep,
                           boolean online, double latitude, double longitude, String message,
                           long updatedAt) {}
