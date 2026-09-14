package com.wikicollection.application.service;

import java.util.List;

import com.wikicollection.domain.model.GameSearchResult;
import com.wikicollection.domain.port.in.GameSearchUseCase;
import com.wikicollection.domain.port.out.ExternalGameCatalogClient;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
public class GameSearchService implements GameSearchUseCase {

    private final ExternalGameCatalogClient rawgClient;
    private final ExternalGameCatalogClient freeToGameClient;

    public GameSearchService(
            @Qualifier("rawgClient") ExternalGameCatalogClient rawgClient,
            @Qualifier("freeToGameClient") ExternalGameCatalogClient freeToGameClient) {
        this.rawgClient = rawgClient;
        this.freeToGameClient = freeToGameClient;
    }

    @Override
    public Page<GameSearchResult> search(String query, Pageable pageable) {
        if (query == null || query.isBlank()) {
            throw new IllegalArgumentException("El parámetro de búsqueda 'name' es obligatorio");
        }
        List<GameSearchResult> results = rawgClient.search(query);
        if (results.isEmpty()) {
            results = freeToGameClient.search(query);
        }
        return PagedResults.slice(results, pageable);
    }
}
