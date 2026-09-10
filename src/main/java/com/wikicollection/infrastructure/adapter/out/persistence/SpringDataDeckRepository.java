package com.wikicollection.infrastructure.adapter.out.persistence;

import java.util.List;

import org.springframework.data.mongodb.repository.MongoRepository;

public interface SpringDataDeckRepository extends MongoRepository<DeckEntity, String> {

    List<DeckEntity> findByName(String name);
}
