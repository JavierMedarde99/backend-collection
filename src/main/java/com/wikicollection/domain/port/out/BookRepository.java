package com.wikicollection.domain.port.out;

import java.util.Optional;

import com.wikicollection.domain.model.Book;
import com.wikicollection.domain.model.BookSearchCriteria;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface BookRepository {

    Page<Book> search(BookSearchCriteria criteria, Pageable pageable);

    Optional<Book> findById(String id);

    Book save(Book book);

    void deleteById(String id);
}
