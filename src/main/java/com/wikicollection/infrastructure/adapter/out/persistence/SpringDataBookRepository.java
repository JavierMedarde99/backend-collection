package com.wikicollection.infrastructure.adapter.out.persistence;

import java.util.Optional;

import com.wikicollection.domain.model.BookStatus;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

public interface SpringDataBookRepository extends MongoRepository<BookEntity, String> {

    Page<BookEntity> findByStatus(BookStatus status, Pageable pageable);

    Page<BookEntity> findByTagsContaining(String tag, Pageable pageable);

    Optional<BookEntity> findByIsbn(String isbn);

    boolean existsByIsbn(String isbn);

    @Query("{$or: ["
            + "{'title': {$regex: ?0, $options: 'i'}},"
            + "{'authors': {$elemMatch: {$regex: ?0, $options: 'i'}}}"
            + "]}")
    Page<BookEntity> searchByTitleOrAuthor(String query, Pageable pageable);
}