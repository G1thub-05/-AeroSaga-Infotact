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

// Non-durable local substitute for Temporal, intended only for UI demos.
@Service
@ConditionalOnProperty(prefix = "aerosaga.temporal", name = "enabled", havingValue = "false")
public class LocalMissionSimulator {
    private static final int ROUTE_POINTS = 15;
    private final FleetService fleet;
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(4);
    private final Map<String, SimulatedMission> missions = new ConcurrentHashMap<>();

    public LocalMissionSimulator(FleetService fleet) {
        this.fleet = fleet;
    }

    public void start(MissionRequest request) {
        SimulatedMission mission = new SimulatedMission(request);
        missions.put(request.missionId(), mission);
        publish(mission, "ACTIVE", "TAKEOFF", "Mission accepted (local demo mode)", request.startLat(),
                request.startLon());
        mission.task = scheduler.scheduleAtFixedRate(() -> advance(mission), 1, 1, TimeUnit.SECONDS);
    }

    public void setOnline(String droneId, boolean online) {
        missions.values().stream().filter(m -> m.request.droneId().equals(droneId) && !m.terminal)
                .forEach(m -> {
                    synchronized (m) {
                        if (m.terminal || m.online.get() == online)
                            return;
                        m.online.set(online);
                        if (!online && m.state != null) {
                            publish(m, "PAUSED", m.state.currentStep(),
                                    "Telemetry lost; local demo is waiting for reconnect", m.state.latitude(),
                                    m.state.longitude());
                        } else if (online && m.state != null && "PAUSED".equals(m.state.status())) {
                            publish(m, "ACTIVE", m.state.currentStep(), "Drone reconnected; local demo resumed",
                                    m.state.latitude(), m.state.longitude());
                        }
                    }
                });
    }

    public void abort(String missionId) {
        SimulatedMission m = missions.get(missionId);
        if (m == null)
            throw new IllegalArgumentException("Mission not found or already closed");
        synchronized (m) {
            if (m.terminal)
                throw new IllegalArgumentException("Mission not found or already closed");
            m.abort.set(true);
        }
    }

    public MissionState state(String missionId) {
        SimulatedMission m = missions.get(missionId);
        if (m == null)
            throw new IllegalArgumentException("Mission not found");
        synchronized (m) {
            if (m.state == null)
                throw new IllegalArgumentException("Mission not found");
            return m.state;
        }
    }

    private void advance(SimulatedMission m) {
        synchronized (m) {
            advanceLocked(m);
        }
    }

    private void advanceLocked(SimulatedMission m) {
        if (m.terminal)
            return;
        Drone drone = fleet.get(m.request.droneId());
        if (!m.online.get() || !drone.online()) {
            if (!"PAUSED".equals(m.state.status()))
                publish(m, "PAUSED", m.state.currentStep(), "Telemetry lost; local demo is waiting for reconnect",
                        m.state.latitude(), m.state.longitude());
            return;
        }
        if ("PAUSED".equals(m.state.status()))
            publish(m, "ACTIVE", m.state.currentStep(), "Drone reconnected; local demo resumed", m.state.latitude(),
                    m.state.longitude());
        if (m.abort.get() && !m.returning) {
            beginReturn(m, "Abort received; returning to base");
            return;
        }
        if (m.index == 0) {
            m.index = 1;
            m.routePoint = 0;
            m.routeStartLat = m.state.latitude();
            m.routeStartLon = m.state.longitude();
            publish(m, "ACTIVE", "NAVIGATE", "En route to delivery destination", m.routeStartLat, m.routeStartLon);
        } else if (m.index == 1) {
            m.routePoint++;
            double p = (double) m.routePoint / ROUTE_POINTS;
            double lat = m.request.startLat() + (m.request.destinationLat() - m.request.startLat()) * p;
            double lon = m.request.startLon() + (m.request.destinationLon() - m.request.startLon()) * p;
            publish(m, "ACTIVE", "NAVIGATE", "En route to delivery destination", lat, lon);
            if (m.routePoint >= ROUTE_POINTS)
                m.index = 2;
        } else if (m.index == 2) {
            if (m.request.simulateDropFailure() && m.failureAttempts < 3) {
                m.failureAttempts++;
                publish(m, "ACTIVE", "DROP_PACKAGE",
                        "Simulated package drop failure; retry " + m.failureAttempts + "/3", m.state.latitude(),
                        m.state.longitude());
            } else {
                beginReturn(m,
                        m.request.simulateDropFailure() ? "Drop failed after retries; compensating by returning to base"
                                : "Package delivered; returning to base");
            }
        } else {
            m.routePoint++;
            double p = (double) m.routePoint / ROUTE_POINTS;
            double lat = m.routeStartLat + (m.request.startLat() - m.routeStartLat) * p;
            double lon = m.routeStartLon + (m.request.startLon() - m.routeStartLon) * p;
            publish(m, "RETURNING", "RETURN_TO_BASE", "Returning to base", lat, lon);
            if (m.routePoint >= ROUTE_POINTS) {
                finish(m, m.abort.get() ? "ABORTED" : m.request.simulateDropFailure() ? "FAILED_SAFE" : "COMPLETED",
                        m.abort.get() ? "Drone safely returned to base after abort"
                                : m.request.simulateDropFailure()
                                        ? "Mission failed; compensation returned drone to base"
                                        : "Package delivered; drone returned");
            }
        }
    }

    private void beginReturn(SimulatedMission m, String message) {
        m.index = 3;
        m.returning = true;
        m.routePoint = 0;
        m.routeStartLat = m.state.latitude();
        m.routeStartLon = m.state.longitude();
        publish(m, "RETURNING", "RETURN_TO_BASE", message, m.routeStartLat, m.routeStartLon);
    }

    private void finish(SimulatedMission m, String status, String message) {
        m.terminal = true;
        m.task.cancel(false);
        publish(m, status, "RETURN_TO_BASE", message, m.request.startLat(), m.request.startLon());
    }

    private void publish(SimulatedMission m, String status, String step, String message, double lat, double lon) {
        MissionRequest r = m.request;
        m.state = new MissionState(r.missionId(), r.droneId(), status, step, m.online.get(), lat, lon, message,
                System.currentTimeMillis());
        fleet.updateMission(r.missionId(), r.droneId(), step, status, lat, lon, message);
    }

    @PreDestroy
    public void stop() {
        scheduler.shutdownNow();
    }

    private static final class SimulatedMission {
        final MissionRequest request;
        final AtomicBoolean online = new AtomicBoolean(true);
        final AtomicBoolean abort = new AtomicBoolean(false);
        volatile MissionState state;
        volatile ScheduledFuture<?> task;
        volatile int index;
        volatile int routePoint;
        volatile int failureAttempts;
        volatile double routeStartLat;
        volatile double routeStartLon;
        volatile boolean returning;
        volatile boolean terminal;

        SimulatedMission(MissionRequest request) {
            this.request = request;
        }
    }
}