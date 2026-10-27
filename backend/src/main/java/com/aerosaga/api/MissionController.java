package com.aerosaga.api;

import java.util.*;

import org.springframework.beans.factory.annotation.Value;
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

import com.aerosaga.telemetry.Drone;
import com.aerosaga.telemetry.FleetService;
import com.aerosaga.temporal.MissionRequest;
import com.aerosaga.temporal.MissionState;
import com.aerosaga.temporal.MissionWorkflow;
import com.aerosaga.persistence.PersistenceService;

import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowOptions;

@RestController              // Marks this class as a REST controller so Spring can handle HTTP requests.
@RequestMapping("/api")   // Sets the common URL prefix for all endpoints in this controller.

// Allows frontend applications running on these origins to call this API.
@CrossOrigin(origins = { "http://localhost:5173", "http://127.0.0.1:5173" })

// Registers this controller when Temporal is enabled. If the property is missing, Temporal mode is enabled by default.
@ConditionalOnProperty(prefix = "aerosaga.temporal", name = "enabled", havingValue = "true", matchIfMissing = true)
public class MissionController {
    private final FleetService fleet;
    private final WorkflowClient temporal;
    private final PersistenceService persistence;
    @Value("${aerosaga.temporal.task-queue}")
    private String taskQueue;

    public MissionController(FleetService fleet, WorkflowClient temporal, PersistenceService persistence) {
        this.fleet = fleet;
        this.temporal = temporal;
        this.persistence = persistence;
    }

	// Health check endpoint to verify that the mission-control API is running.
    @GetMapping("/health")
    public Map<String, String> health() {
        return Map.of("status", "UP", "service", "AeroSaga Mission Control", "mode", "TEMPORAL");
    }

	// Returns the list of drones managed by the fleet service.
    @GetMapping("/fleet")
    public List<Drone> fleet() {
        return fleet.all();
    }

	// Returns all persisted mission states.
    @GetMapping("/missions")
    public List<MissionState> missions() {
        return persistence.allMissionStates();
    }

	// Accepts a mission request, validates it, persists it, and starts a Temporal workflow asynchronously.
    @PostMapping("/missions")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public Map<String, String> launch(@RequestBody LaunchRequest input) {

	    // Validate destination coordinates:
	    // Latitude: -90 se 90 tak — North/South position.
	    // Longitude: -180 se 180 tak — East/West position.
        if (!Double.isFinite(input.destinationLat())
		        || input.destinationLat() < -90
		        || input.destinationLat() > 90
                || !Double.isFinite(input.destinationLon())
                || input.destinationLon() < -180
		        || input.destinationLon() > 180)

	        // Return HTTP 400 Bad Request for invalid destination coordinates.
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Destination latitude/longitude is out of range");

	    // Retrieve the requested drone from the fleet for this mission. Any exception from fleet.get() is handled according to FleetService behavior.
	    Drone drone;
	    try {
		    drone = fleet.get(input.droneId());
	    } catch (NoSuchElementException e) {
		    throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage());
	    }


	    // A mission cannot be launched if the selected drone is offline.
        if (!drone.online()) throw new ResponseStatusException(HttpStatus.CONFLICT, "Drone is offline");

	    // Prevent a drone from starting another mission while it is already active, paused, or returning.
        if (Set.of("ACTIVE", "PAUSED", "RETURNING").contains(drone.status())) throw new ResponseStatusException(HttpStatus.CONFLICT, "Drone already has an active mission");

	    // Generate a unique mission ID using the drone ID and a random UUID. Only the first eight characters of the UUID are included.
        String id = "mission-" + drone.id() + "-" + UUID.randomUUID().toString().substring(0, 8);

	    // Use the supplied source coordinates when available. Otherwise, use the drone's current location as the starting point.
        double startLat = input.sourceLat() == null ? drone.latitude() : input.sourceLat();
        double startLon = input.sourceLon() == null ? drone.longitude() : input.sourceLon();

	    // Validate the resolved starting coordinates before creating the mission.
        validateCoordinate(startLat, startLon, "Source");

	    // Build the mission request containing the mission ID, drone ID, start coordinates, destination coordinates and failure-simulation option.
        MissionRequest request = new MissionRequest(id, drone.id(), startLat, startLon, input.destinationLat(), input.destinationLon(), input.simulateDropFailure());

	    // Persist the mission before starting the workflow.
		persistence.createMission(request);

	    // Create a Temporal workflow stub with the configured task queue and the mission ID as the workflow ID.
        MissionWorkflow workflow = temporal.newWorkflowStub(MissionWorkflow.class, WorkflowOptions.newBuilder().setWorkflowId(id).setTaskQueue(taskQueue).build());

	    // Start the workflow asynchronously; this call does not wait for the mission to finish.
        WorkflowClient.start(workflow::runMission, request);

	    // Return the mission and drone IDs along with the initial launch status.
        return Map.of("missionId", id, "droneId", drone.id(), "status", "STARTED");
    }

	// Marks a drone as offline and notifies its running workflow.
    @PostMapping("/drones/{id}/offline")
    public Drone offline(@PathVariable String id) {
        Drone d = fleet.setOnline(id, false);          // Update the drone's online status in the fleet service.
        signal(id, w -> w.setOnline(false));  // Notify the associated workflow, if applicable, that the drone is offline.
        return d;                                             // Return the updated drone information.
    }

	// Reconnects a drone and notifies its running workflow.
    @PostMapping("/drones/{id}/reconnect")
    public Drone reconnect(@PathVariable String id) {
        Drone d = fleet.setOnline(id, true);          // Update the drone's online status in the fleet service.
        signal(id, w -> w.setOnline(true));  // Notify the associated workflow that the drone is online again.
        return d;                                            // Return the updated drone information.
    }

	// Requests emergency abortion of a mission through its Temporal workflow.
    @PostMapping("/missions/{id}/abort")
    public Map<String, String> abort(@PathVariable String id) {
        try {
            temporal.newWorkflowStub(MissionWorkflow.class, id).emergencyAbort();         // Create a workflow stub for the given mission and send the abort request.
            return Map.of("missionId", id, "status", "ABORT_REQUESTED");    // Confirm that the abort request was sent.
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Mission not found or already closed");
        }
    }

	// Returns the current state of a mission.
    @GetMapping("/missions/{id}")
    public MissionState state(@PathVariable String id) {
        try {
            return temporal.newWorkflowStub(MissionWorkflow.class, id).getState();   // First, try to retrieve the state directly from the Temporal workflow.
        } catch (Exception e) {
	        // If the workflow state cannot be retrieved, fall back to persisted mission data. Return HTTP 404 if the mission is not present in persistence either.
            return persistence.findMissionState(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Mission not found"));
        }
    }

	// Sends an online/offline signal to the workflow associated with a drone.
    private void signal(String droneId, java.util.function.Consumer<MissionWorkflow> action) {

	    // Retrieve the latest drone information.
        Drone d = fleet.get(droneId);

	    // Send the signal only if the drone has a mission ID and its status indicates that the workflow may still need the online/offline update.
        if (d.missionId() != null && Set.of("ACTIVE", "PAUSED", "RETURNING", "OFFLINE").contains(d.status())) {
            try {
	            // Create a workflow stub using the drone's current mission ID, then execute the supplied action (online or offline signal).
                action.accept(temporal.newWorkflowStub(MissionWorkflow.class, d.missionId()));
            }
			catch (Exception ignored) {
				// Intentionally ignore signaling failures, such as a closed workflow.
            }
        }
    }

	// Validates source or other geographic coordinates.
    private void validateCoordinate(double lat, double lon, String name) {
        if (!Double.isFinite(lat) || lat < -90 || lat > 90 || !Double.isFinite(lon) || lon < -180 || lon > 180)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, name + " latitude/longitude is out of range");
    }

	// Defines the JSON request structure accepted by POST /api/missions. sourceLat and sourceLon are optional; if null, the drone's current location is used.
    public record LaunchRequest(String droneId, double destinationLat, double destinationLon,
            boolean simulateDropFailure, Double sourceLat, Double sourceLon) {
    }
}
