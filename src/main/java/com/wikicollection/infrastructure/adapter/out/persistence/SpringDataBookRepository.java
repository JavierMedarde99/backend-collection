package com.wikicollection.infrastructure.adapter.out.persistence;

import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;

public interface SpringDataBookRepository extends MongoRepository<BookEntity, String> {

    Optional<BookEntity> findByExternalId(String externalId);
}
