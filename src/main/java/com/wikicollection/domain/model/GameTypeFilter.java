package com.wikicollection.domain.model;

/**
 * Filtro de listado {@code GET /games?type=base|dlc|all}. No hace falta un enum de tipo como en
 * BGG: aquí no hay vocabulario externo que mapear, porque la pertenencia de un juego a su base
 * la define haber entrado por el flujo de adiciones.
 */
public enum GameTypeFilter {

    /** Solo juegos base: los que no son contenido adicional (incluye los registros antiguos). */
    BASE,

    /** Solo contenido adicional. */
    DLC,

    /** Sin filtro. */
    ALL
}
