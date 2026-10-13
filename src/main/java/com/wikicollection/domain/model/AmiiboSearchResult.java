package com.wikicollection.domain.model;

import java.time.LocalDate;
import java.util.List;

/**
 * Resultado de la búsqueda externa en AmiiboAPI: subconjunto del catálogo
 * con los datos mínimos para poder añadir el amiibo a la colección. El campo
 * {@code externalId} es {@code head + tail} (16 hex), la clave natural del
 * catálogo.
 */
public record AmiiboSearchResult(
        String externalId,
        String name,
        String character,
        String amiiboSeries,
        String gameSeries,
        AmiiboType amiiboType,
        AmiiboReleaseDates releaseDates,
        String imageUrl,
        List<String> compatibleGames,
        String externalSource) {

    public static final String EXTERNAL_SOURCE = "amiiboapi";
}