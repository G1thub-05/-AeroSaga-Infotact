package com.aerosaga.telemetry;

import com.aerosaga.temporal.MissionState;
import com.aerosaga.persistence.PersistenceService;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;

@Service public class FleetService {
	private final Map<String, Drone> drones = new ConcurrentHashMap<>();
	private final Set<FleetSocketHandler> sockets = new CopyOnWriteArraySet<>();
	private final PersistenceService persistence;

	public FleetService(PersistenceService persistence) {
		this.persistence = persistence;
		Random random = new Random(42);
		for(int i = 1; i <= 50; i++) {
			int index = i - 1;
			int row = index / 10;
			int column = index % 10;
			double lat = 21.2514 + (row - 2) * 0.002;
			double lon = 81.6296 + (column - 4.5) * 0.002;
			Drone initial = new Drone(id(i), String.format("Aero-%02d", i), "IDLE", lat, lon, 45 + random.nextInt(55), null,
					"STANDBY", true, "Ready for dispatch");
			Drone existing = persistence.findDrone(initial.id()).orElse(null);
			drones.put(initial.id(), existing == null ? initial : existing);
			if (existing == null) persistence.saveDrone(initial);
		}
	}

	public List<Drone> all() {
		return drones.values().stream().sorted(Comparator.comparing(Drone::id)).toList();
	}

	public Drone get(String id) {
		Drone d = drones.get(id);
		if(d == null)
			throw new NoSuchElementException("Drone not found: " + id);
		return d;
	}

	public synchronized void updateMission(String missionId, String droneId, String step, String status, double lat, double lon, String message) {
		Drone old = get(droneId);
		Drone next = new Drone(old.id(), old.name(), status, lat, lon, old.battery(), missionId, step, old.online(),
				message);
		drones.put(droneId, next);
		persistence.saveDrone(next);
		MissionState state = new MissionState(missionId, droneId, status, step, old.online(), lat, lon, message,
				System.currentTimeMillis());
		persistence.saveMissionState(state);
		broadcast(Map.of("type", "drone", "drone", next, "mission", state));
	}

	public synchronized Drone setOnline(String id, boolean online) {
		Drone old = get(id);
		String status = online ? (old.missionId() != null && !Set.of("COMPLETED", "ABORTED", "FAILED_SAFE")
				.contains(old.status()) ? "PAUSED" : "IDLE") : "OFFLINE";
		Drone next =
				new Drone(old.id(), old.name(), status, old.latitude(), old.longitude(), old.battery(), old.missionId(),
						old.currentStep(), online, online ? "Telemetry restored" : "Telemetry lost");
		drones.put(id, next);
		persistence.saveDrone(next);
		broadcast(Map.of("type", "drone", "drone", next));
		return next;
	}

	public void addSocket(FleetSocketHandler handler) {
		sockets.add(handler);
	}

	public void removeSocket(FleetSocketHandler handler) {
		sockets.remove(handler);
	}

	public void broadcast(Object event) {
		sockets.forEach(s -> s.send(event));
	}

	private String id(int i) {
		return String.format("drone-%02d", i);
	}
}
