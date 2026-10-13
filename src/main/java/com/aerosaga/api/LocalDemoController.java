package com.aerosaga.api;

import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.UUID;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.aerosaga.demo.LocalMissionSimulator;
import com.aerosaga.telemetry.Drone;
import com.aerosaga.telemetry.FleetService;
import com.aerosaga.temporal.MissionRequest;
import com.aerosaga.temporal.MissionState;

@RestController
@RequestMapping("/api") 
@CrossOrigin(origins = "http://localhost:5173")
@ConditionalOnProperty(prefix = "aerosaga.temporal", name = "enabled", havingValue = "false")
public class LocalDemoController {
	private final FleetService fleet;
	private final LocalMissionSimulator simulator;

	public LocalDemoController(FleetService fleet, LocalMissionSimulator simulator) {
		this.fleet = fleet;
		this.simulator = simulator;
	}

	@GetMapping("/health")
	public Map<String, String> health() {
		return Map.of(
				"status",
				"UP",
				"service",
				"AeroSaga local demo (Temporal disabled)",
				"mode",
				"LOCAL_SIMULATION");
	}

	@GetMapping("/fleet")
	public List<Drone> fleet() {
		return fleet.all();
	}

	@PostMapping("/missions")
	@ResponseStatus(HttpStatus.ACCEPTED)
	public Map<String, String> launch(@RequestBody MissionController.LaunchRequest input) {
		if (!Double.isFinite(input.destinationLat())
				|| input.destinationLat() < -90
				|| input.destinationLat() > 90
				|| !Double.isFinite(input.destinationLon())
				|| input.destinationLon() < -180
				|| input.destinationLon() > 180)
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Destination latitude/longitude is out of range");
		Drone drone;
		try {
			drone = fleet.get(input.droneId());
		} catch (NoSuchElementException e) {
			throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage());
		}
		if (!drone.online())
			throw new ResponseStatusException(HttpStatus.CONFLICT, "Drone is offline");
		if (Set.of("ACTIVE", "PAUSED", "RETURNING").contains(drone.status()))
			throw new ResponseStatusException(HttpStatus.CONFLICT, "Drone already has an active mission");
		String id = "mission-" + drone.id() + "-" + UUID.randomUUID().toString().substring(0, 8);
		double startLat = input.sourceLat() == null ? drone.latitude() : input.sourceLat();
		double startLon = input.sourceLon() == null ? drone.longitude() : input.sourceLon();
		if (!Double.isFinite(startLat) || startLat < -90 || startLat > 90 || !Double.isFinite(
				startLon) || startLon < -180 || startLon > 180)
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Source latitude/longitude is out of range");
		simulator.start(
				new MissionRequest(id, drone.id(), startLat, startLon, input.destinationLat(), input.destinationLon(),
						input.simulateDropFailure()));
		return Map.of("missionId", id, "droneId", drone.id(), "status", "STARTED", "mode", "LOCAL_SIMULATION");
	}

	@PostMapping("/drones/{id}/offline")
	public Drone offline(@PathVariable String id) {
		Drone drone = setOnline(id, false);
		simulator.setOnline(id, false);
		return drone;
	}

	@PostMapping("/drones/{id}/reconnect")
	public Drone reconnect(@PathVariable String id) {
		Drone drone = setOnline(id, true);
		simulator.setOnline(id, true);
		return drone;
	}

	private Drone setOnline(String id, boolean online) {
		try {
			return fleet.setOnline(id, online);
		} catch (NoSuchElementException e) {
			throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage());
		}
	}

	@PostMapping("/missions/{id}/abort")
	public Map<String, String> abort(@PathVariable String id) {
		try {
			simulator.abort(id);
			return Map.of("missionId", id, "status", "ABORT_REQUESTED");
		} catch (IllegalArgumentException e) {
			throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage());
		}
	}

	@GetMapping("/missions/{id}")
	public MissionState state(@PathVariable String id) {
		try {
			return simulator.state(id);
		} catch (IllegalArgumentException e) {
			throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage());
		}
	}
}
