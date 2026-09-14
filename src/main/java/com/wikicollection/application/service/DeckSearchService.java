package com.wikicollection.application.service;

import com.wikicollection.domain.model.MagicCardSearchResult;
import com.wikicollection.domain.port.in.DeckSearchUseCase;
import com.wikicollection.domain.port.out.ExternalMagicCardCatalogClient;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
public class DeckSearchService implements DeckSearchUseCase {

    private final ExternalMagicCardCatalogClient catalogClient;

    public DeckSearchService(ExternalMagicCardCatalogClient catalogClient) {
        this.catalogClient = catalogClient;
    }

    @Override
    public Page<MagicCardSearchResult> searchCommanders(String colors, Pageable pageable) {
        String filter = colors == null ? "" : colors.trim();
        return PagedResults.slice(catalogClient.searchCommanders(filter), pageable);
    }
}
