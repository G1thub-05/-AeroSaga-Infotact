package com.aerosaga.api;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

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

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = { "http://localhost:5173", "http://127.0.0.1:5173" })
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

    @GetMapping("/health")
    public Map<String, String> health() {
        return Map.of("status", "UP", "service", "AeroSaga Mission Control", "mode", "TEMPORAL");
    }

    @GetMapping("/fleet")
    public List<Drone> fleet() {
        return fleet.all();
    }

    @GetMapping("/missions")
    public List<MissionState> missions() {
        return persistence.allMissionStates();
    }

    @PostMapping("/missions")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public Map<String, String> launch(@RequestBody LaunchRequest input) {
        if (!Double.isFinite(
                input.destinationLat()) || input.destinationLat() < -90 || input.destinationLat() > 90
                || !Double.isFinite(
                        input.destinationLon())
                || input.destinationLon() < -180 || input.destinationLon() > 180)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Destination latitude/longitude is out of range");
        Drone drone = fleet.get(input.droneId());
        if (!drone.online())
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Drone is offline");
        if (Set.of("ACTIVE", "PAUSED", "RETURNING").contains(drone.status()))
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Drone already has an active mission");
        String id = "mission-" + drone.id() + "-" + UUID.randomUUID().toString().substring(0, 8);
        double startLat = input.sourceLat() == null ? drone.latitude() : input.sourceLat();
        double startLon = input.sourceLon() == null ? drone.longitude() : input.sourceLon();
        validateCoordinate(startLat, startLon, "Source");
        MissionRequest request = new MissionRequest(id, drone.id(), startLat, startLon, input.destinationLat(),
                input.destinationLon(),
                input.simulateDropFailure());
        persistence.createMission(request);
        MissionWorkflow workflow = temporal.newWorkflowStub(MissionWorkflow.class,
                WorkflowOptions.newBuilder().setWorkflowId(id).setTaskQueue(taskQueue).build());
        WorkflowClient.start(workflow::runMission, request);
        return Map.of("missionId", id, "droneId", drone.id(), "status", "STARTED");
    }

    @PostMapping("/drones/{id}/offline")
    public Drone offline(@PathVariable String id) {
        Drone d = fleet.setOnline(id, false);
        signal(id, w -> w.setOnline(false));
        return d;
    }

    @PostMapping("/drones/{id}/reconnect")
    public Drone reconnect(@PathVariable String id) {
        Drone d = fleet.setOnline(id, true);
        signal(id, w -> w.setOnline(true));
        return d;
    }

    @PostMapping("/missions/{id}/abort")
    public Map<String, String> abort(@PathVariable String id) {
        try {
            temporal.newWorkflowStub(MissionWorkflow.class, id).emergencyAbort();
            return Map.of("missionId", id, "status", "ABORT_REQUESTED");
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Mission not found or already closed");
        }
    }

    @GetMapping("/missions/{id}")
    public MissionState state(@PathVariable String id) {
        try {
            return temporal.newWorkflowStub(MissionWorkflow.class, id).getState();
        } catch (Exception e) {
            return persistence.findMissionState(id)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Mission not found"));
        }
    }

    private void signal(String droneId, java.util.function.Consumer<MissionWorkflow> action) {
        Drone d = fleet.get(droneId);
        if (d.missionId() != null && Set.of("ACTIVE", "PAUSED", "RETURNING", "OFFLINE").contains(d.status())) {
            try {
                action.accept(temporal.newWorkflowStub(MissionWorkflow.class, d.missionId()));
            } catch (Exception ignored) {
            }
        }
    }

    private void validateCoordinate(double lat, double lon, String name) {
        if (!Double.isFinite(lat) || lat < -90 || lat > 90 || !Double.isFinite(lon) || lon < -180 || lon > 180)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, name + " latitude/longitude is out of range");
    }

    public record LaunchRequest(String droneId, double destinationLat, double destinationLon,
            boolean simulateDropFailure, Double sourceLat, Double sourceLon) {
    }
}
