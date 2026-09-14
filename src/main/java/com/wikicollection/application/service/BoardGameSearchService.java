package com.wikicollection.application.service;

import com.wikicollection.domain.model.BoardGameSearchResult;
import com.wikicollection.domain.port.in.BoardGameSearchUseCase;
import com.wikicollection.domain.port.out.ExternalBoardGameCatalogClient;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
public class BoardGameSearchService implements BoardGameSearchUseCase {

    private final ExternalBoardGameCatalogClient bggXmlClient;

    public BoardGameSearchService(ExternalBoardGameCatalogClient bggXmlClient) {
        this.bggXmlClient = bggXmlClient;
    }

    @Override
    public Page<BoardGameSearchResult> search(String query, Pageable pageable) {
        if (query == null || query.isBlank()) {
            throw new IllegalArgumentException("El parámetro de búsqueda 'name' es obligatorio");
        }
        return PagedResults.slice(bggXmlClient.search(query), pageable);
    }
}
