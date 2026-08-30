package com.wikicollection.domain.port.out;

import java.util.Optional;

import com.wikicollection.domain.model.Book;
import com.wikicollection.domain.model.BookStatus;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface BookRepository {

    Page<Book> findAll(Pageable pageable);

    Optional<Book> findById(String id);

    Page<Book> findByStatus(BookStatus status, Pageable pageable);

    Page<Book> findByTagsContaining(String tag, Pageable pageable);

    Optional<Book> findByIsbn(String isbn);

    Page<Book> searchByTitleOrAuthor(String query, Pageable pageable);

    Book save(Book book);

    void deleteById(String id);
}