package com.aerosaga.persistence;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;

import com.aerosaga.telemetry.Drone;
import com.aerosaga.temporal.MissionRequest;
import com.aerosaga.temporal.MissionState;

@Service
public class PersistenceService {
    private final DroneRepository drones;
    private final MissionRepository missions;

    public PersistenceService(DroneRepository drones, MissionRepository missions) {
        this.drones = drones;
        this.missions = missions;
    }

    public Optional<Drone> findDrone(String id) {
        return drones.findById(id).map(DroneEntity::toDrone);
    }

    public void saveDrone(Drone drone) {
        DroneEntity entity = drones.findById(drone.id()).orElseGet(DroneEntity::new);
        entity.update(drone);
        drones.save(entity);
    }

    public void createMission(MissionRequest request) {
        missions.save(new MissionEntity(request));
    }

    public void saveMissionState(MissionState state) {
        MissionEntity entity = missions.findById(state.missionId()).orElse(null);
        if (entity != null) {
            entity.update(state);
            missions.save(entity);
        }
    }

    public Optional<MissionState> findMissionState(String id) {
        return missions.findById(id).map(MissionEntity::toState);
    }

    public List<MissionState> allMissionStates() {
        return missions.findAll().stream().map(MissionEntity::toState).toList();
    }
}
