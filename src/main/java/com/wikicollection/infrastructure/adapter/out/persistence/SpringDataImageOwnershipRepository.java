package com.wikicollection.infrastructure.adapter.out.persistence;

import org.springframework.data.mongodb.repository.MongoRepository;

public interface SpringDataImageOwnershipRepository extends MongoRepository<ImageOwnershipEntity, String> {
}
