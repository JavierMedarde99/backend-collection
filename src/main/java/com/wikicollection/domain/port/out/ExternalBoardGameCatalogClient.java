package com.wikicollection.domain.port.out;

import java.util.List;

import com.wikicollection.domain.model.BoardGameExpansionInfo;
import com.wikicollection.domain.model.BoardGameSearchResult;

public interface ExternalBoardGameCatalogClient {

    List<BoardGameSearchResult> search(String query);

    /**
     * Expansiones que cuelgan de un juego base, según el catálogo de BGG.
     *
     * <p>A diferencia de {@link #search(String)}, <b>no</b> se traga el error del
     * catálogo: si BGG no contesta se propaga {@code ExternalCatalogException} para
     * que la API responda 502 en lugar de 200 con una lista vacía.
     *
     * @param baseBggId bggId del juego base
     * @return las expansiones conocidas; vacío si el base no tiene o BGG no lo conoce
     */
    List<BoardGameExpansionInfo> expansionsOf(String baseBggId);

    /**
     * Fichas de BGG para unos bggIds concretos. Los ids que BGG no conoce no
     * aparecen en la respuesta (no son un error). Lanza
     * {@code ExternalCatalogException} si BGG falla.
     */
    List<BoardGameExpansionInfo> findByBggIds(List<String> bggIds);
}
