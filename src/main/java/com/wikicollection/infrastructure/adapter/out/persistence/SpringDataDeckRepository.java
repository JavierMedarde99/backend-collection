package com.wikicollection.infrastructure.adapter.out.persistence;

import java.util.Collection;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface SpringDataDeckRepository extends MongoRepository<DeckEntity, String> {

    Page<DeckEntity> findByName(String name, Pageable pageable);

    Page<DeckEntity> findByOwnerId(String ownerId, Pageable pageable);

    Page<DeckEntity> findByOwnerIdNotIn(Collection<String> ownerIds, Pageable pageable);

    Page<DeckEntity> findByNameAndOwnerId(String name, String ownerId, Pageable pageable);

    Page<DeckEntity> findByNameAndOwnerIdNotIn(String name, Collection<String> ownerIds, Pageable pageable);
}
