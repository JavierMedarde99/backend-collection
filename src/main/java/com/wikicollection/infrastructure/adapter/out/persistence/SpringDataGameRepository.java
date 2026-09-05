package com.wikicollection.infrastructure.adapter.out.persistence;

import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;

public interface SpringDataGameRepository extends MongoRepository<GameEntity, String> {

    Optional<GameEntity> findByExternalId(String externalId);
}