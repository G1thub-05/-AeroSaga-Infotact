package com.aerosaga.weather;

import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import com.fasterxml.jackson.annotation.JsonProperty;

@Service
public class WeatherService {
    private final RestClient client = RestClient.create("https://api.open-meteo.com");

    public WeatherResponse current(double lat, double lon) {
        OpenMeteoResponse response = client.get()
                .uri(uri -> uri.path("/v1/forecast")
                        .queryParam("latitude", lat)
                        .queryParam("longitude", lon)
                        .queryParam(
                                "current",
                                "temperature_2m,wind_speed_10m,visibility,weather_code")
                        .queryParam("timezone", "auto")
                        .build())
                .retrieve()
                .body(OpenMeteoResponse.class);

        if (response == null || response.current() == null) {
            throw new IllegalStateException("Weather provider returned no data");
        }

        var current = response.current();

        return new WeatherResponse(
                condition(current.weatherCode()),
                current.temperatureC(),
                current.windSpeedKmh(),
                current.visibilityMeters() / 1000.0,
                current.time());
    }

    private String condition(int code) {
        if (code == 0)
            return "Clear";
        if (code <= 3)
            return "Partly cloudy";
        if (code == 45 || code == 48)
            return "Fog";
        if (code >= 51 && code <= 67)
            return "Rain";
        if (code >= 71 && code <= 77)
            return "Snow";
        if (code >= 80 && code <= 82)
            return "Rain showers";
        if (code >= 95)
            return "Thunderstorm";
        return "Cloudy";
    }

    public record OpenMeteoResponse(Current current) {
        public record Current(
                @JsonProperty("temperature_2m") double temperatureC,

                @JsonProperty("wind_speed_10m") double windSpeedKmh,

                @JsonProperty("visibility") double visibilityMeters,

                @JsonProperty("weather_code") int weatherCode,

                @JsonProperty("time") String time) {
        }
    }
}