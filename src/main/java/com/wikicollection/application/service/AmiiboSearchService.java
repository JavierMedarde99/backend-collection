package com.wikicollection.application.service;

import com.wikicollection.domain.model.AmiiboSearchResult;
import com.wikicollection.domain.port.in.AmiiboSearchUseCase;
import com.wikicollection.domain.port.out.ExternalAmiiboCatalogClient;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
public class AmiiboSearchService implements AmiiboSearchUseCase {

    private final ExternalAmiiboCatalogClient amiiboCatalogClient;

    public AmiiboSearchService(ExternalAmiiboCatalogClient amiiboCatalogClient) {
        this.amiiboCatalogClient = amiiboCatalogClient;
    }

    @Override
    public Page<AmiiboSearchResult> search(String query, Pageable pageable) {
        if (query == null || query.isBlank()) {
            throw new IllegalArgumentException("El parámetro de búsqueda 'name' es obligatorio");
        }
        return PagedResults.slice(amiiboCatalogClient.searchByName(query), pageable);
    }
}