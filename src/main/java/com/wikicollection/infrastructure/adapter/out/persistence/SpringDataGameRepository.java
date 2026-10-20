package com.wikicollection.infrastructure.adapter.out.persistence;

import java.util.List;
import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;

public interface SpringDataGameRepository extends MongoRepository<GameEntity, String> {

    Optional<GameEntity> findByExternalId(String externalId);

    List<GameEntity> findByOwnerIdAndExternalIdIn(String ownerId, List<String> externalIds);

    void deleteByOwnerIdAndParentId(String ownerId, String parentId);
}