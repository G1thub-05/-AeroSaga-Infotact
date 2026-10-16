package com.aerosaga.telemetry;

import com.aerosaga.temporal.MissionState;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;

@Service public class FleetService {
	private final Map<String, Drone> drones = new ConcurrentHashMap<>();
	private final Set<FleetSocketHandler> sockets = new CopyOnWriteArraySet<>();

	public FleetService() {
		Random random = new Random(42);
		for(int i = 1; i <= 50; i++) {
			int index = i - 1;
			int row = index / 10;
			int column = index % 10;
			double lat = 21.2514 + (row - 2) * 0.002;
			double lon = 81.6296 + (column - 4.5) * 0.002;
			drones.put(id(i),
					new Drone(id(i), String.format("Aero-%02d", i), "IDLE", lat, lon, 45 + random.nextInt(55), null,
							"STANDBY", true, "Ready for dispatch"));
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
		broadcast(Map.of("type", "drone", "drone", next, "mission",
				new MissionState(missionId, droneId, status, step, old.online(), lat, lon, message,
						System.currentTimeMillis())));
	}
}
