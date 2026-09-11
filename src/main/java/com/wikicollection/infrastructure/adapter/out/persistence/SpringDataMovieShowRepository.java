package com.wikicollection.infrastructure.adapter.out.persistence;

import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;

public interface SpringDataMovieShowRepository extends MongoRepository<MovieShowEntity, String> {

    Optional<MovieShowEntity> findByExternalId(String externalId);
}
