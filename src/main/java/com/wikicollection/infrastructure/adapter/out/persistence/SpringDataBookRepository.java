package com.wikicollection.infrastructure.adapter.out.persistence;

import org.springframework.data.mongodb.repository.MongoRepository;

public interface SpringDataBookRepository extends MongoRepository<BookEntity, String> {
}
