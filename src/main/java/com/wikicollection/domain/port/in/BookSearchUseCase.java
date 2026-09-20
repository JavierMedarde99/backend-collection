package com.wikicollection.domain.port.in;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.wikicollection.domain.model.BookSearchResult;

public interface BookSearchUseCase {

    Page<BookSearchResult> search(String query, Pageable pageable);

    Page<BookSearchResult> searchByIsbn(String isbn, Pageable pageable);
}