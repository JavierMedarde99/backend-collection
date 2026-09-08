package com.wikicollection.application.service;

import java.util.List;

import com.wikicollection.domain.model.MagicCard;
import com.wikicollection.domain.model.MagicCardSearchResult;
import com.wikicollection.domain.port.in.MagicCardSearchUseCase;
import com.wikicollection.domain.port.out.ExternalMagicCardCatalogClient;

import org.springframework.stereotype.Service;

@Service
public class MagicCardSearchService implements MagicCardSearchUseCase {

    private static final int MAX_RESULTS = 10;

    private final ExternalMagicCardCatalogClient scryfallClient;

    public MagicCardSearchService(ExternalMagicCardCatalogClient scryfallClient) {
        this.scryfallClient = scryfallClient;
    }

    @Override
    public List<MagicCardSearchResult> search(String query) {
        if (query == null || query.isBlank()) {
            throw new IllegalArgumentException("El parámetro de búsqueda 'name' es obligatorio");
        }
        List<MagicCardSearchResult> results = scryfallClient.search(query);
        return results.size() > MAX_RESULTS ? results.subList(0, MAX_RESULTS) : results;
    }

    @Override
    public MagicCard findByName(String name) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("El parámetro de búsqueda 'name' es obligatorio");
        }
        return scryfallClient.findByName(name);
    }
}
