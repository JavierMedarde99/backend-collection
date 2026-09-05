package com.wikicollection.infrastructure.adapter.out.persistence;

import java.util.Optional;

import com.wikicollection.domain.model.GameStatus;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface SpringDataGameRepository extends MongoRepository<GameEntity, String> {

    Page<GameEntity> findByStatus(GameStatus status, Pageable pageable);

    Optional<GameEntity> findByExternalId(String externalId);
}