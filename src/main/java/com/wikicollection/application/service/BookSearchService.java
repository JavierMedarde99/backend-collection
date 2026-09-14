package com.wikicollection.application.service;

import com.wikicollection.domain.model.BookSearchResult;
import com.wikicollection.domain.port.in.BookSearchUseCase;
import com.wikicollection.domain.port.out.ExternalBookCatalogClient;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
public class BookSearchService implements BookSearchUseCase {

    private final ExternalBookCatalogClient externalBookCatalogClient;

    public BookSearchService(ExternalBookCatalogClient externalBookCatalogClient) {
        this.externalBookCatalogClient = externalBookCatalogClient;
    }

    @Override
    public Page<BookSearchResult> search(String query, Pageable pageable) {
        if (query == null || query.isBlank()) {
            throw new IllegalArgumentException("El parámetro de búsqueda 'q' es obligatorio");
        }
        return PagedResults.slice(externalBookCatalogClient.search(query), pageable);
    }
}
