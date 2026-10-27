package com.aerosaga.demo;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import com.aerosaga.telemetry.Drone;
import com.aerosaga.telemetry.FleetService;
import com.aerosaga.temporal.MissionRequest;
import com.aerosaga.temporal.MissionState;

import jakarta.annotation.PreDestroy;

// Registers this class as a Spring service. This is a non-durable local substitute for Temporal, intended only for UI demonstrations.
@Service

// Creates this controller only when aerosaga.temporal.enabled is false.
@ConditionalOnProperty(prefix = "aerosaga.temporal", name = "enabled", havingValue = "false")
public class LocalMissionSimulator {

	// Number of intermediate points used to simulate the drone's route.
    private static final int ROUTE_POINTS = 15;
    private final FleetService fleet;

	// Runs scheduled mission updates using a pool of four worker threads.
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(4);

	// Stores active and completed simulated missions by mission ID. ConcurrentHashMap allows safe access from multiple threads.
    private final Map<String, SimulatedMission> missions = new ConcurrentHashMap<>();
    public LocalMissionSimulator(FleetService fleet) {
        this.fleet = fleet;
    }

	// Starts a new simulated mission and schedules periodic progress updates.
    public void start(MissionRequest request) {
        SimulatedMission mission = new SimulatedMission(request);
        missions.put(request.missionId(), mission);

	    // Publish the initial mission state at the starting coordinates.
        publish(mission, "ACTIVE", "TAKEOFF", "Mission accepted (local demo mode)", request.startLat(), request.startLon());

	    // Advance the simulated mission once per second after an initial one-second delay.
        mission.task = scheduler.scheduleAtFixedRate(() -> advance(mission), 1, 1, TimeUnit.SECONDS);
    }

	// Updates a drone's connectivity state in every non-terminal mission associated with it.
    public void setOnline(String droneId, boolean online) {
        missions.values().stream().filter(m -> m.request.droneId().equals(droneId) && !m.terminal)
                .forEach(m -> {
                    synchronized (m) {

	                    // Ignore terminal missions or requests that do not change connectivity.
                        if (m.terminal || m.online.get() == online) return;

	                    // Update the simulator's connectivity flag.
                        m.online.set(online);


	                    // Pause the mission when the drone goes offline.
                        if (!online && m.state != null) {
                            publish(m, "PAUSED", m.state.currentStep(), "Telemetry lost; local demo is waiting for " + "reconnect", m.state.latitude(), m.state.longitude());
                        }

                        // Resume a paused mission when the drone reconnects.
						else if (online && m.state != null && "PAUSED".equals(m.state.status())) {
                            publish(m, "ACTIVE", m.state.currentStep(), "Drone reconnected; local demo resumed", m.state.latitude(), m.state.longitude());
                        }
                    }
                });
    }

	// Requests an active simulated mission to abort and return to base.
    public void abort(String missionId) {

	    // Find the mission by its ID.
        SimulatedMission m = missions.get(missionId);

	    // Reject requests for missing missions.
        if (m == null) throw new IllegalArgumentException("Mission not found or already closed");
        synchronized (m) {

	        // Reject abort requests for missions that have already finished.
            if (m.terminal) throw new IllegalArgumentException("Mission not found or already closed");

	        // Set the abort flag; the scheduled update will process it.
            m.abort.set(true);
        }
    }

	// Returns the latest in-memory state of a simulated mission.
    public MissionState state(String missionId) {

	    // Find the mission by ID.
        SimulatedMission m = missions.get(missionId);

	    // Throw an exception if the mission does not exist in memory.
        if (m == null) throw new IllegalArgumentException("Mission not found");

        synchronized (m) {

	        // Ensure the mission has an initialized state.
            if (m.state == null) throw new IllegalArgumentException("Mission not found");

	        // Return the most recently published state.
            return m.state;
        }
    }

	// Synchronizes access before advancing a mission.
    private void advance(SimulatedMission m) {
        synchronized (m) {
            advanceLocked(m);
        }
    }

	// Advances the mission by one simulation step. The caller must hold the mission's monitor lock.
    private void advanceLocked(SimulatedMission m) {

	    // Do nothing if the mission has already reached a terminal state.
        if (m.terminal) return;

	    // Get the latest drone information from the fleet service.
        Drone drone = fleet.get(m.request.droneId());

	    // Pause progress if either the simulator or fleet service reports the drone offline.
        if (!m.online.get() || !drone.online()) {

	        // Publish the paused state only when the mission is not already paused.
            if (!"PAUSED".equals(m.state.status()))
                publish(m, "PAUSED", m.state.currentStep(), "Telemetry lost; local demo is waiting for reconnect", m.state.latitude(), m.state.longitude());
            return;
        }

	    // Resume the mission if connectivity has been restored.
        if ("PAUSED".equals(m.state.status()))
            publish(m, "ACTIVE", m.state.currentStep(), "Drone reconnected; local demo resumed", m.state.latitude(), m.state.longitude());

	    // If an abort was requested, start the return journey unless already returning.
		if (m.abort.get() && !m.returning) {
            beginReturn(m, "Abort received; returning to base");
            return;
        }

	    // Step 0: begin navigation after the initial takeoff state.
        if (m.index == 0) {
            m.index = 1;
            m.routePoint = 0;

	        // Save the route's starting coordinates.
            m.routeStartLat = m.state.latitude();
            m.routeStartLon = m.state.longitude();

	        // Publish the navigation state.
            publish(m, "ACTIVE", "NAVIGATE", "En route to delivery destination", m.routeStartLat, m.routeStartLon);
        }

        // Step 1: move gradually toward the destination.
		else if (m.index == 1) {
            m.routePoint++;

	        // Calculate the fraction of the route completed, from 0 to 1.
            double p = (double) m.routePoint / ROUTE_POINTS;

	        // Interpolate latitude and longitude between the source and destination.
            double lat = m.request.startLat() + (m.request.destinationLat() - m.request.startLat()) * p;
            double lon = m.request.startLon() + (m.request.destinationLon() - m.request.startLon()) * p;

	        // Publish the drone's simulated position.
            publish(m, "ACTIVE", "NAVIGATE", "En route to delivery destination", lat, lon);

	        // Move to the delivery step after reaching the final route point.
            if (m.routePoint >= ROUTE_POINTS)
                m.index = 2;
        }

        // Step 2: simulate package delivery or package-drop failures.
		else if (m.index == 2) {

	        // Simulate up to three failed drop attempts when failure simulation is enabled.
            if (m.request.simulateDropFailure() && m.failureAttempts < 3) {
                m.failureAttempts++;

	            // Publish the current retry attempt.
                publish(m, "ACTIVE", "DROP_PACKAGE", "Simulated package drop failure; retry " + m.failureAttempts + "/3", m.state.latitude(), m.state.longitude());
            }

            // Start the return journey after delivery or after all simulated retries fail.
			else {
                beginReturn(
						m,
		                m.request.simulateDropFailure()
		                ? "Drop failed after retries; compensating by " + "returning to base"
		                : "Package delivered; returning to base"
                );
            }
        }

        // Step 3: move the drone back toward its starting coordinates.
		else {
            m.routePoint++;

	        // Calculate the return-route progress.
            double p = (double) m.routePoint / ROUTE_POINTS;

	        // Interpolate from the saved return-start position back to the mission's source.
            double lat = m.routeStartLat + (m.request.startLat() - m.routeStartLat) * p;
            double lon = m.routeStartLon + (m.request.startLon() - m.routeStartLon) * p;

	        // Publish the drone's current return position.
            publish(m, "RETURNING", "RETURN_TO_BASE", "Returning to base", lat, lon);

	        // Finish the mission after the return route is complete.
            if (m.routePoint >= ROUTE_POINTS) {
                finish(
						m,
		                m.abort.get()
				                ? "ABORTED" : m.request.simulateDropFailure()
				                ? "FAILED_SAFE" : "COMPLETED", m.abort.get()
				                ? "Drone safely returned to base after abort" : m.request.simulateDropFailure()
				                ? "Mission failed; compensation returned drone to base" : "Package delivered; drone returned");
            }
        }
    }

	// Initializes the return-to-base phase from the drone's current position.
    private void beginReturn(SimulatedMission m, String message) {

        m.index = 3;                 // Set the simulation index to the return phase.
        m.returning = true;          // Mark that the drone is returning so an abort is not processed repeatedly.
        m.routePoint = 0;            // Reset the counter for the return route.

	    // Store the current position as the return journey's starting point.
        m.routeStartLat = m.state.latitude();
        m.routeStartLon = m.state.longitude();

	    // Publish the return status and explanatory message.
        publish(m, "RETURNING", "RETURN_TO_BASE", message, m.routeStartLat, m.routeStartLon);
    }

	// Marks a mission as finished and publishes its terminal state.
    private void finish(SimulatedMission m, String status, String message) {
        m.terminal = true;                       // Prevent further mission progress.
        m.task.cancel(false); // Cancel future scheduled updates without interrupting a running task.

	    // Publish the final mission state at the starting coordinates.
        publish(m, status, "RETURN_TO_BASE", message, m.request.startLat(), m.request.startLon());
    }

	// Creates and publishes the latest mission state.
    private void publish(SimulatedMission m, String status, String step, String message, double lat, double lon) {

	    // Get the original mission request.
        MissionRequest r = m.request;

	    // Construct a state snapshot with status, step, connectivity, position, message, and the current timestamp.
        m.state = new MissionState(r.missionId(), r.droneId(), status, step, m.online.get(), lat, lon, message, System.currentTimeMillis());

	    // Update the fleet service with the latest mission information.
        fleet.updateMission(r.missionId(), r.droneId(), step, status, lat, lon, message);
    }

	// Update the fleet service with the latest mission information.
    @PreDestroy
    public void stop() {

	    // Stop scheduled tasks and shut down the simulator's worker threads.
        scheduler.shutdownNow();
    }

	// Holds the mutable in-memory data for one simulated mission.
    private static final class SimulatedMission {
        final MissionRequest request;                                     // Immutable request details for this mission.
        final AtomicBoolean online = new AtomicBoolean(true);   // Thread-safe flag indicating whether the simulated drone is online.
        final AtomicBoolean abort = new AtomicBoolean(false);   // Thread-safe flag used to request mission abortion.

		// Latest published state and scheduled task reference.
        volatile MissionState state;
        volatile ScheduledFuture<?> task;

        volatile int index;                // Simulation phase: 0 = initial, 1 = outbound, 2 = delivery, 3 = return.
        volatile int routePoint;           // Current point number along the outbound or return route.
        volatile int failureAttempts;      // Number of simulated package-drop failures so far.

		// Coordinates from which the current route segment started.
        volatile double routeStartLat;
        volatile double routeStartLon;


        volatile boolean returning;        // Indicates that the drone has entered the return phase.
        volatile boolean terminal;         // Indicates that the mission has reached a terminal state.

		// Initialize a simulated mission with its original request.
        SimulatedMission(MissionRequest request) {
            this.request = request;
        }
    }
}