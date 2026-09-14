package com.wikicollection.application.service;

import java.util.List;

import com.wikicollection.domain.model.BookSearchResult;
import com.wikicollection.infrastructure.config.CacheConfig;
import com.wikicollection.domain.port.in.BookSearchUseCase;
import com.wikicollection.domain.port.out.ExternalBookCatalogClient;

import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

@Service
public class BookSearchService implements BookSearchUseCase {

    private final ExternalBookCatalogClient externalBookCatalogClient;

    public BookSearchService(ExternalBookCatalogClient externalBookCatalogClient) {
        this.externalBookCatalogClient = externalBookCatalogClient;
    }

    @Override
    @Cacheable(cacheNames = CacheConfig.BOOK_SEARCH, key = "#query")
    public List<BookSearchResult> search(String query) {
        if (query == null || query.isBlank()) {
            throw new IllegalArgumentException("El parámetro de búsqueda 'q' es obligatorio");
        }
        return externalBookCatalogClient.search(query);
    }
}