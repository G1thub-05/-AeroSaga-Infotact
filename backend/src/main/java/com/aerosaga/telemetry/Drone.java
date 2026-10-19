package com.aerosaga.telemetry;

public record Drone(String id, String name, String status, double latitude, double longitude, int battery,
                    String missionId, String currentStep, boolean online, String message) {
}
