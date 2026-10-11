package com.wikicollection.application.service;

import java.util.List;

import com.wikicollection.domain.model.BookSearchResult;
import com.wikicollection.domain.port.in.BookSearchUseCase;
import com.wikicollection.domain.port.out.ExternalBookCatalogClient;
import com.wikicollection.domain.port.out.ExternalBookEnricher;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
public class BookSearchService implements BookSearchUseCase {

    private final ExternalBookCatalogClient externalBookCatalogClient;
    private final ExternalBookEnricher externalBookEnricher;

    public BookSearchService(ExternalBookCatalogClient externalBookCatalogClient,
                             ExternalBookEnricher externalBookEnricher) {
        this.externalBookCatalogClient = externalBookCatalogClient;
        this.externalBookEnricher = externalBookEnricher;
    }

    @Override
    public Page<BookSearchResult> search(String query, Pageable pageable) {
        if (query == null || query.isBlank()) {
            throw new IllegalArgumentException("El parámetro de búsqueda 'q' es obligatorio");
        }
        return externalBookCatalogClient.search(query, pageable);
    }

    @Override
    public Page<BookSearchResult> searchByIsbn(String isbn, Pageable pageable) {
        String clean = normalizeIsbn(isbn);
        if (clean.isEmpty()) {
            throw new IllegalArgumentException("El parámetro 'isbn' es obligatorio");
        }
        List<BookSearchResult> results = externalBookCatalogClient.searchByIsbn(clean).stream()
                .map(this::enrichOrKeep)
                .toList();
        return PagedResults.slice(results, pageable);
    }

    /** Open Library rellena portada/descripción cuando Google no las trae; si falla, se conserva el original. */
    private BookSearchResult enrichOrKeep(BookSearchResult result) {
        BookSearchResult enriched = externalBookEnricher.enrich(result);
        return enriched != null ? enriched : result;
    }

    private static String normalizeIsbn(String isbn) {
        if (isbn == null) {
            return "";
        }
        return isbn.replaceAll("[\\s-]", "");
    }
}
