package com.wikicollection.application.service;

import com.wikicollection.domain.model.MovieMediaType;
import com.wikicollection.domain.model.MovieSearchResult;
import com.wikicollection.domain.port.in.MovieSearchUseCase;
import com.wikicollection.domain.port.out.ExternalMovieCatalogClient;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
public class MovieSearchService implements MovieSearchUseCase {

    private final ExternalMovieCatalogClient catalogClient;

    public MovieSearchService(ExternalMovieCatalogClient catalogClient) {
        this.catalogClient = catalogClient;
    }

    @Override
    public Page<MovieSearchResult> search(String query, Pageable pageable) {
        return search(query, null, pageable);
    }

    @Override
    public Page<MovieSearchResult> search(String query, MovieMediaType mediaType, Pageable pageable) {
        if (query == null || query.isBlank()) {
            throw new IllegalArgumentException("El parámetro de búsqueda 'name' es obligatorio");
        }
        return PagedResults.slice(catalogClient.search(query, mediaType), pageable);
    }
}
