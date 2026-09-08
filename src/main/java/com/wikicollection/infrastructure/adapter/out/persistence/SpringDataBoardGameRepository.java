package com.wikicollection.infrastructure.adapter.out.persistence;

import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;

public interface SpringDataBoardGameRepository extends MongoRepository<BoardGameEntity, String> {

    Optional<BoardGameEntity> findByBggId(String bggId);
}
