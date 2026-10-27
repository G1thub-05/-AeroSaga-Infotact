package com.aerosaga.weather;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@CrossOrigin(origins = { "http://localhost:5173", "http://127.0.0.1:5173" })
@RequestMapping("/api/weather")
public class WeatherController {
    private final WeatherService weather;

    public WeatherController(WeatherService weather) {
        this.weather = weather;
    }

    @GetMapping
    public WeatherResponse current(
            @RequestParam double lat,
            @RequestParam double lon) {
        if (!Double.isFinite(lat) || lat < -90 || lat > 90
                || !Double.isFinite(lon) || lon < -180 || lon > 180) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Invalid latitude or longitude");
        }

        try {
            return weather.current(lat, lon);

        } catch (Exception e) {
            e.printStackTrace(); // asli error backend terminal mein dikhega
            throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY,
                    "Weather provider is unavailable: " + e.getMessage());
        }
    }
}