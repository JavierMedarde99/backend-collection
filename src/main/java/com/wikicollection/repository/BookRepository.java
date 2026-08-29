package com.wikicollection.repository;

import java.util.Optional;

import com.wikicollection.model.Book;
import com.wikicollection.model.BookStatus;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

public interface BookRepository extends MongoRepository<Book, String> {

    Page<Book> findByStatus(BookStatus status, Pageable pageable);

    Page<Book> findByTagsContaining(String tag, Pageable pageable);

    Optional<Book> findByIsbn(String isbn);

    boolean existsByIsbn(String isbn);

    @Query("{$or: ["
            + "{'title': {$regex: ?0, $options: 'i'}},"
            + "{'authors': {$elemMatch: {$regex: ?0, $options: 'i'}}}"
            + "]}")
    Page<Book> searchByTitleOrAuthor(String query, Pageable pageable);
}