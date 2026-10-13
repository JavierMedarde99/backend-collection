package com.wikicollection.infrastructure.adapter.out.persistence;

import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;

public interface SpringDataFunkoRepository extends MongoRepository<FunkoEntity, String> {

    Optional<FunkoEntity> findByExternalId(String externalId);
}
