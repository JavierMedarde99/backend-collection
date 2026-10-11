package com.wikicollection.domain.port.out;

import com.wikicollection.domain.model.BookSearchResult;

/**
 * Rellena metadatos que el catálogo principal (Google Books) no aporta para un
 * resultado concreto, como la portada o la descripción, usando una fuente
 * secundaria (Open Library). Devuelve un resultado nuevo con los huecos
 * rellenados; si no hay fuente o no hace falta, devuelve el mismo resultado.
 */
public interface ExternalBookEnricher {

    BookSearchResult enrich(BookSearchResult result);
}