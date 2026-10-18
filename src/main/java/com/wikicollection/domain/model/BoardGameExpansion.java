package com.wikicollection.domain.model;

/**
 * Una expansión tal y como la sirve {@code GET /boardgames/{id}/expansions}: el
 * catálogo de BGG más el flag de si el espectador ya la tiene en su colección.
 *
 * @param inCollection solo lo calcula el servicio con los registros del usuario;
 *                     no es parte del catálogo de BGG y por eso se cachea fuera
 */
public record BoardGameExpansion(
        String bggId,
        String title,
        Integer yearPublished,
        String imageUrl,
        String thumbnailUrl,
        boolean inCollection) {
}
