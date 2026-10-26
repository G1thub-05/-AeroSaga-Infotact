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
import com.aerosaga.persistence.PersistenceService;

@RestController
@RequestMapping("/api")

// Allows frontend applications running on these local development
@CrossOrigin(origins = { "http://localhost:5173", "http://127.0.0.1:5173" })

// Creates this controller only when aerosaga.temporal.enabled is false.
@ConditionalOnProperty(prefix = "aerosaga.temporal", name = "enabled", havingValue = "false")
public class LocalDemoController {
	private final FleetService fleet;
	private final LocalMissionSimulator simulator;
	private final PersistenceService persistence;

	public LocalDemoController(FleetService fleet, LocalMissionSimulator simulator, PersistenceService persistence) {
		this.fleet = fleet;
		this.simulator = simulator;
		this.persistence = persistence;
	}

	// Returns basic information about the backend's health and operating mode.
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

	// Returns the current list of drones managed by FleetService.
	@GetMapping("/fleet")
	public List<Drone> fleet() {
		return fleet.all();
	}

	// Returns the persisted states of all missions.
	@GetMapping("/missions")
	public List<MissionState> missions() {
		return persistence.allMissionStates();
	}

	// Starts a new mission using the local simulator.
	@PostMapping("/missions")
	@ResponseStatus(HttpStatus.ACCEPTED)
	public Map<String, String> launch(@RequestBody MissionController.LaunchRequest input) {

		// Validate destination latitude and longitude.
		if (!Double.isFinite(input.destinationLat())
				|| input.destinationLat() < -90
				|| input.destinationLat() > 90
				|| !Double.isFinite(input.destinationLon())
				|| input.destinationLon() < -180
				|| input.destinationLon() > 180)

			// Return HTTP 400 Bad Request for invalid destination coordinates.
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Destination latitude/longitude is out of range");

		// Retrieve the requested drone from the fleet.
		Drone drone;
		try {
			drone = fleet.get(input.droneId());
		} catch (NoSuchElementException e) {
			throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage());
		}

		// A mission cannot be launched if the drone is offline.
		if (!drone.online()) throw new ResponseStatusException(HttpStatus.CONFLICT, "Drone is offline");

		// Prevent a drone from starting another mission while it is already active, paused, or returning.
		if (Set.of("ACTIVE", "PAUSED", "RETURNING").contains(drone.status())) throw new ResponseStatusException(HttpStatus.CONFLICT, "Drone already has an active mission");

		// Generate a unique mission ID using the drone ID and a random UUID. Only the first eight characters of the UUID are included.
		String id = "mission-" + drone.id() + "-" + UUID.randomUUID().toString().substring(0, 8);

		// Use the supplied source coordinates when available. Otherwise, use the drone's current location as the starting point.
		double startLat = input.sourceLat() == null ? drone.latitude() : input.sourceLat();
		double startLon = input.sourceLon() == null ? drone.longitude() : input.sourceLon();

		// Validate the selected source coordinates before creating the mission.
		if (!Double.isFinite(startLat) || startLat < -90 || startLat > 90 || !Double.isFinite(startLon) || startLon < -180 || startLon > 180)
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Source latitude/longitude is out of range");

		// Build the mission request with its ID, drone, source, destination, and optional simulated drop-failure flag.
		MissionRequest request = new MissionRequest(id, drone.id(), startLat, startLon, input.destinationLat(), input.destinationLon(), input.simulateDropFailure());

		// Persist the mission request before starting the simulation.
		persistence.createMission(request);

		// Start local mission execution.
		simulator.start(request);

		// Return mission details to the client. The controller responds with HTTP 202 Accepted.
		return Map.of("missionId", id, "droneId", drone.id(), "status", "STARTED", "mode", "LOCAL_SIMULATION");
	}

	// Marks the specified drone as offline and updates the local simulator.
	@PostMapping("/drones/{id}/offline")
	public Drone offline(@PathVariable String id) {
		Drone drone = setOnline(id, false); // Update the drone's online state in the fleet service.
		simulator.setOnline(id, false);     // Notify the simulator so it can account for the offline state.
		return drone;                              // Return the updated drone information.
	}

	// Marks the specified drone as online and updates the local simulator.
	@PostMapping("/drones/{id}/reconnect")
	public Drone reconnect(@PathVariable String id) {
		Drone drone = setOnline(id, true);  // Update the drone's online state in the fleet service.
		simulator.setOnline(id, true);      // Notify the simulator that the drone is online again.
		return drone;                              // Return the updated drone information.
	}

	// Updates a drone's online/offline state through FleetService.
	private Drone setOnline(String id, boolean online) {
		try {
			return fleet.setOnline(id, online);
		} catch (NoSuchElementException e) {
			throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage());
		}
	}

	// Requests cancellation of a running local mission.
	@PostMapping("/missions/{id}/abort")
	public Map<String, String> abort(@PathVariable String id) {
		try {
			simulator.abort(id);
			return Map.of("missionId", id, "status", "ABORT_REQUESTED");
		} catch (IllegalArgumentException e) {
			throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage());
		}
	}

	// Returns the current state of a specific mission.
	@GetMapping("/missions/{id}")
	public MissionState state(@PathVariable String id) {
		try {
			return simulator.state(id);
		} catch (IllegalArgumentException e) {
			return persistence.findMissionState(id)
					.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage()));
		}
	}
}