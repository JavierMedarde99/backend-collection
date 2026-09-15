package com.wikicollection.infrastructure.adapter.out.persistence;

import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;

public interface SpringDataUserRepository extends MongoRepository<UserEntity, String> {

    Optional<UserEntity> findByUsername(String username);

    Optional<UserEntity> findByEmail(String email);

    boolean existsByUsername(String username);

    boolean existsByEmail(String email);
}
