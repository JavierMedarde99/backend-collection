package com.wikicollection.domain.model;

/**
 * Filtro de listado {@code GET /boardgames?type=base|expansion|all}. Es
 * independiente de {@link BoardGameType}: ese enum mapea el vocabulario de BGG y
 * no contempla «todo», que es lo que pide el listado por defecto.
 */
public enum BoardGameTypeFilter {

    /** Solo juegos base: los que no son expansión (incluye los registros antiguos, sin tipo). */
    BASE,

    /** Solo expansiones. */
    EXPANSION,

    /** Sin filtro. */
    ALL
}
