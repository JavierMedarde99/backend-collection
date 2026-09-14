package com.wikicollection.application.service;

import java.util.List;

import com.wikicollection.domain.model.GameSearchResult;
import com.wikicollection.infrastructure.config.CacheConfig;
import com.wikicollection.domain.port.in.GameSearchUseCase;
import com.wikicollection.domain.port.out.ExternalGameCatalogClient;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

@Service
public class GameSearchService implements GameSearchUseCase {

    private static final int MAX_RESULTS = 5;

    private final ExternalGameCatalogClient rawgClient;
    private final ExternalGameCatalogClient freeToGameClient;

    public GameSearchService(
            @Qualifier("rawgClient") ExternalGameCatalogClient rawgClient,
            @Qualifier("freeToGameClient") ExternalGameCatalogClient freeToGameClient) {
        this.rawgClient = rawgClient;
        this.freeToGameClient = freeToGameClient;
    }

    @Override
    @Cacheable(cacheNames = CacheConfig.GAME_SEARCH, key = "#query")
    public List<GameSearchResult> search(String query) {
        if (query == null || query.isBlank()) {
            throw new IllegalArgumentException("El parámetro de búsqueda 'name' es obligatorio");
        }
        List<GameSearchResult> results = rawgClient.search(query);
        if (results.isEmpty()) {
            results = freeToGameClient.search(query);
        }
        return results.size() > MAX_RESULTS ? results.subList(0, MAX_RESULTS) : results;
    }
}