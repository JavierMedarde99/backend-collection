package com.wikicollection.application.service;

import java.util.List;

import com.wikicollection.domain.model.FunkoCategory;
import com.wikicollection.domain.model.FunkoSearchResult;
import com.wikicollection.domain.port.in.FunkoSearchUseCase;
import com.wikicollection.domain.port.out.ExternalFunkoCatalogClient;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

/**
 * Búsqueda en el catálogo externo de Funko Pop. La fuente exige al menos 2
 * caracteres ({@code q} de 1 carácter responde 422), así que la validación se
 * hace aquí y se traduce a un 400 propio sin llegar a llamarla. La búsqueda
 * externa no pagina: el corte de página lo hace {@link PagedResults}.
 */
@Service
public class FunkoSearchService implements FunkoSearchUseCase {

    static final int MIN_QUERY_LENGTH = 2;

    private final ExternalFunkoCatalogClient funkoCatalogClient;

    public FunkoSearchService(ExternalFunkoCatalogClient funkoCatalogClient) {
        this.funkoCatalogClient = funkoCatalogClient;
    }

    @Override
    public Page<FunkoSearchResult> search(String query, Pageable pageable) {
        if (query == null || query.isBlank()) {
            throw new IllegalArgumentException("El parámetro de búsqueda 'name' es obligatorio");
        }
        if (query.trim().length() < MIN_QUERY_LENGTH) {
            throw new IllegalArgumentException(
                    "El parámetro de búsqueda 'name' necesita al menos " + MIN_QUERY_LENGTH + " caracteres");
        }
        return PagedResults.slice(funkoCatalogClient.searchByName(query), pageable);
    }

    @Override
    public List<FunkoCategory> listCategories() {
        return funkoCatalogClient.listCategories();
    }
}
