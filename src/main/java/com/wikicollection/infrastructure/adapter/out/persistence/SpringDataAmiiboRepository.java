package com.wikicollection.infrastructure.adapter.out.persistence;

import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;

public interface SpringDataAmiiboRepository extends MongoRepository<AmiiboEntity, String> {

    Optional<AmiiboEntity> findByExternalId(String externalId);
}
