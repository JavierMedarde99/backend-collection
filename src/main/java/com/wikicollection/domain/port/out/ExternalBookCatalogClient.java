package com.wikicollection.domain.port.out;

import java.util.List;

import com.wikicollection.domain.model.BookSearchResult;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface ExternalBookCatalogClient {

    Page<BookSearchResult> search(String query, Pageable pageable);

    List<BookSearchResult> searchByIsbn(String isbn);

    List<String> getCategories(String volumeId);
}
