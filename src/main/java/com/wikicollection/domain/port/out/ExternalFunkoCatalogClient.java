package com.wikicollection.domain.port.out;

import java.util.List;
import java.util.Optional;

import com.wikicollection.domain.model.FunkoCategory;
import com.wikicollection.domain.model.FunkoSearchResult;

/**
 * Catálogo externo de Funko Pop (HobbyQuantHQ). La búsqueda por nombre no
 * pagina: la fuente devuelve como mucho su límite de resultados y el corte de
 * página es competencia del servicio. Todas las llamadas exigen el header
 * {@code x-api-key}.
 */
public interface ExternalFunkoCatalogClient {

    /**
     * Busca por nombre (mínimo 2 caracteres en la fuente). Un 200 con
     * {@code {"results": []}} es un resultado vacío válido, no un error.
     */
    List<FunkoSearchResult> searchByName(String query);

    /**
     * Recupera un Pop por su id externo. HobbyQuantHQ devuelve 404 cuando el id
     * no existe; se traduce a {@code Optional.empty()} (vacío ≠ error de red).
     */
    Optional<FunkoSearchResult> findById(String externalId);

    /**
     * Categorías del catálogo ({@code [{name,count}]}) para el filtro del
     * frontend.
     */
    List<FunkoCategory> listCategories();
}
