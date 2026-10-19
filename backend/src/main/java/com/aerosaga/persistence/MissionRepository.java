package com.aerosaga.persistence;

import org.springframework.data.mongodb.repository.MongoRepository;

public interface MissionRepository extends MongoRepository<MissionEntity, String> {
}
