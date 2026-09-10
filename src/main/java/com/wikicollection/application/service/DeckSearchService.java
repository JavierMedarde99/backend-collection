package com.wikicollection.application.service;

import java.util.List;

import com.wikicollection.domain.model.MagicCardSearchResult;
import com.wikicollection.domain.port.in.DeckSearchUseCase;
import com.wikicollection.domain.port.out.ExternalMagicCardCatalogClient;

import org.springframework.stereotype.Service;

@Service
public class DeckSearchService implements DeckSearchUseCase {

    private static final int MAX_RESULTS = 10;

    private final ExternalMagicCardCatalogClient catalogClient;

    public DeckSearchService(ExternalMagicCardCatalogClient catalogClient) {
        this.catalogClient = catalogClient;
    }

    @Override
    public List<MagicCardSearchResult> searchCommanders(String colors) {
        String filter = colors == null ? "" : colors.trim();
        List<MagicCardSearchResult> results = catalogClient.searchCommanders(filter);
        return results.size() > MAX_RESULTS ? results.subList(0, MAX_RESULTS) : results;
    }
}
