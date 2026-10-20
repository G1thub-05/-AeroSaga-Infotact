package com.aerosaga.temporal;

public record MissionRequest(String missionId, String droneId, double startLat, double startLon,
                             double destinationLat, double destinationLon, boolean simulateDropFailure) {}
