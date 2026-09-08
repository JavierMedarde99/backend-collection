package com.wikicollection.application.service;

import java.util.List;

import com.wikicollection.domain.model.BoardGameSearchResult;
import com.wikicollection.domain.port.in.BoardGameSearchUseCase;
import com.wikicollection.domain.port.out.ExternalBoardGameCatalogClient;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

@Service
public class BoardGameSearchService implements BoardGameSearchUseCase {

    private static final int MAX_RESULTS = 10;

    private final ExternalBoardGameCatalogClient bggJsonClient;
    private final ExternalBoardGameCatalogClient bggXmlClient;

    public BoardGameSearchService(
            @Qualifier("bggJsonClient") ExternalBoardGameCatalogClient bggJsonClient,
            @Qualifier("bggXmlClient") ExternalBoardGameCatalogClient bggXmlClient) {
        this.bggJsonClient = bggJsonClient;
        this.bggXmlClient = bggXmlClient;
    }

    @Override
    public List<BoardGameSearchResult> search(String query) {
        if (query == null || query.isBlank()) {
            throw new IllegalArgumentException("El parámetro de búsqueda 'name' es obligatorio");
        }
        List<BoardGameSearchResult> results = bggJsonClient.search(query);
        if (results.isEmpty()) {
            results = bggXmlClient.search(query);
        }
        return results.size() > MAX_RESULTS ? results.subList(0, MAX_RESULTS) : results;
    }
}