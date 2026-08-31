package com.wikicollection.infrastructure.adapter.out.persistence;

import com.wikicollection.domain.model.BookState;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface SpringDataBookRepository extends MongoRepository<BookEntity, String> {

    Page<BookEntity> findByState(BookState state, Pageable pageable);
}
