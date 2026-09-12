package com.wikicollection.infrastructure.adapter.out.persistence;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface SpringDataDeckRepository extends MongoRepository<DeckEntity, String> {

    Page<DeckEntity> findByName(String name, Pageable pageable);
}
