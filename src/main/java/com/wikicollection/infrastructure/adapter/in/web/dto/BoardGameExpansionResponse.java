package com.wikicollection.infrastructure.adapter.in.web.dto;

import com.wikicollection.domain.model.BoardGameExpansion;

/**
 * Una expansión del catálogo de BGG con el flag de si el espectador ya la
 * tiene. El flag lo calcula el servicio con los registros del usuario: no viene
 * del catálogo y por eso se cachea fuera.
 */
public record BoardGameExpansionResponse(
        String bggId,
        String title,
        Integer yearPublished,
        String imageUrl,
        String thumbnailUrl,
        boolean inCollection) {

    public static BoardGameExpansionResponse from(BoardGameExpansion expansion) {
        if (expansion == null) {
            return null;
        }
        return new BoardGameExpansionResponse(
                expansion.bggId(),
                expansion.title(),
                expansion.yearPublished(),
                expansion.imageUrl(),
                expansion.thumbnailUrl(),
                expansion.inCollection());
    }
}
