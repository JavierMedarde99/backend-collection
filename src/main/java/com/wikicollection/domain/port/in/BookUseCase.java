package com.wikicollection.domain.port.in;

import com.wikicollection.domain.model.Book;
import com.wikicollection.domain.model.BookStatus;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface BookUseCase {

    Page<Book> findAll(Pageable pageable);

    Page<Book> findByStatus(BookStatus status, Pageable pageable);

    Page<Book> findByTag(String tag, Pageable pageable);

    Page<Book> searchByTitleOrAuthor(String query, Pageable pageable);

    Book findById(String id);

    Book save(Book book);

    Book update(String id, Book updates);

    void delete(String id);
}