package com.wikicollection.domain.model;

import java.math.BigDecimal;
import java.util.List;

/**
 * Una entrada del catálogo de BoardGameGeek relevante para la relación
 * base ↔ expansión: lo que hace falta para pintar la lista de expansiones de un
 * juego y para dar de alta una expansión con el mismo bloque de campos que ya
 * rellena la búsqueda.
 *
 * @param bggId      id de la entrada en BGG
 * @param type       tipo BGG de la entrada; {@code null} si BGG devuelve un tipo
 *                   que no modelamos (no es una expansión)
 * @param baseBggIds bggId de los juegos de los que depende (links
 *                   {@code boardgameexpansion} con {@code inbound="true"})
 */
public record BoardGameExpansionInfo(
        String bggId,
        String title,
        String description,
        Integer yearPublished,
        Integer minPlayers,
        Integer maxPlayers,
        Integer minPlaytime,
        Integer maxPlaytime,
        String publisher,
        List<String> designers,
        List<String> categories,
        List<String> mechanics,
        String imageUrl,
        String thumbnailUrl,
        BigDecimal bggRating,
        BoardGameType type,
        List<String> baseBggIds) {
}
