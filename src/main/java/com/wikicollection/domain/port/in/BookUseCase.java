package com.wikicollection.domain.port.in;

import com.wikicollection.domain.model.Book;
import com.wikicollection.domain.model.BookState;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface BookUseCase {

    Page<Book> findAll(Pageable pageable);

    Page<Book> findByState(BookState state, Pageable pageable);

    Book findById(String id);

    Book save(Book book);

    Book update(String id, Book updates);

    void delete(String id);
}
