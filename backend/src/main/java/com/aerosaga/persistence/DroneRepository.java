package com.aerosaga.persistence;

import org.springframework.data.mongodb.repository.MongoRepository;

public interface DroneRepository extends MongoRepository<DroneEntity, String> {
}
