package com.wikicollection.application.service;

import com.wikicollection.domain.model.MagicCard;
import com.wikicollection.domain.model.MagicCardSearchResult;
import com.wikicollection.domain.port.in.MagicCardSearchUseCase;
import com.wikicollection.domain.port.out.ExternalMagicCardCatalogClient;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
public class MagicCardSearchService implements MagicCardSearchUseCase {

    private final ExternalMagicCardCatalogClient scryfallClient;

    public MagicCardSearchService(ExternalMagicCardCatalogClient scryfallClient) {
        this.scryfallClient = scryfallClient;
    }

    @Override
    public Page<MagicCardSearchResult> search(String query, Pageable pageable) {
        if (query == null || query.isBlank()) {
            throw new IllegalArgumentException("El parámetro de búsqueda 'name' es obligatorio");
        }
        return PagedResults.slice(scryfallClient.search(query), pageable);
    }

    @Override
    public MagicCard findByName(String name) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("El parámetro de búsqueda 'name' es obligatorio");
        }
        return scryfallClient.findByName(name);
    }
}
