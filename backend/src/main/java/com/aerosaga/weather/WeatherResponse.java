package com.aerosaga.weather;

public record WeatherResponse(
        String condition,
        double temperatureC,
        double windSpeedKmh,
        double visibilityKm,
        String observedAt) {
}