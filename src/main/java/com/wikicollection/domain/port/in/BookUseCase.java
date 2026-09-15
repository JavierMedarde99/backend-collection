package com.wikicollection.domain.port.in;

import com.wikicollection.domain.model.Book;
import com.wikicollection.domain.model.BookSearchCriteria;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface BookUseCase {

    Page<Book> search(BookSearchCriteria criteria, Pageable pageable);

    Book findById(String id);

    Book save(Book book, String ownerId);

    Book update(String id, Book updates, String userId);

    void delete(String id, String userId);
}
