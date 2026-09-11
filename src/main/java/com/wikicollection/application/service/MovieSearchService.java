package com.wikicollection.application.service;

import java.util.List;

import com.wikicollection.domain.model.MovieSearchResult;
import com.wikicollection.domain.port.in.MovieSearchUseCase;
import com.wikicollection.domain.port.out.ExternalMovieCatalogClient;

import org.springframework.stereotype.Service;

@Service
public class MovieSearchService implements MovieSearchUseCase {

    private static final int MAX_RESULTS = 10;

    private final ExternalMovieCatalogClient catalogClient;

    public MovieSearchService(ExternalMovieCatalogClient catalogClient) {
        this.catalogClient = catalogClient;
    }

    @Override
    public List<MovieSearchResult> search(String query) {
        if (query == null || query.isBlank()) {
            throw new IllegalArgumentException("El parámetro de búsqueda 'name' es obligatorio");
        }
        List<MovieSearchResult> results = catalogClient.search(query);
        return results.size() > MAX_RESULTS ? results.subList(0, MAX_RESULTS) : results;
    }
}
