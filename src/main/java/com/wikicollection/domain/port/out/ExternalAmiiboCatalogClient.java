package com.wikicollection.domain.port.out;

import java.util.List;
import java.util.Optional;

import com.wikicollection.domain.model.AmiiboSearchResult;

/**
 * Catálogo externo de amiibo (AmiiboAPI). Sin auth, sin paginación: la
 * búsqueda devuelve todos los que coinciden con el nombre (coincidencia
 * parcial) y el detalle por id externo devuelve un único resultado.
 */
public interface ExternalAmiiboCatalogClient {

    /**
     * Busca pelo nombre (coincidencia parcial, insensible). Un 200 con
     * {@code {"amiibo": []}} es un resultado vacío válido, no un error.
     */
    List<AmiiboSearchResult> searchByName(String query);

    /**
     * Recupera un amiibo por su id externo (head+tail). AmiiboAPI devuelve un
     * objeto único para {@code ?id=} y un 400 cuando el id no existe; ambos se
     * traducen a {@code Optional.empty()} (vacío ≠ error de red).
     */
    Optional<AmiiboSearchResult> findById(String externalId);
}