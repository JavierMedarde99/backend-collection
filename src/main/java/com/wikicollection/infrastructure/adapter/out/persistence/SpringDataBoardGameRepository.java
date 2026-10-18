package com.wikicollection.infrastructure.adapter.out.persistence;

import java.util.List;
import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;

/**
 * No hay {@code findByBggId(String)} a secas: en un modelo multi-usuario varios
 * dueños pueden tener el mismo juego, así que el bggId solo identifica dentro de
 * un ownerId.
 */
public interface SpringDataBoardGameRepository extends MongoRepository<BoardGameEntity, String> {

    Optional<BoardGameEntity> findByOwnerIdAndBggId(String ownerId, String bggId);

    List<BoardGameEntity> findByOwnerIdAndBggIdIn(String ownerId, List<String> bggIds);

    List<BoardGameEntity> findByOwnerIdAndParentId(String ownerId, String parentId);

    void deleteByOwnerIdAndParentId(String ownerId, String parentId);
}
