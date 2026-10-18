package com.wikicollection.domain.model;

import java.util.List;

/**
 * Catálogo de expansiones que BGG cuelga de un juego base.
 *
 * @param baseGameId id local del juego base en la colección
 * @param baseBggId  bggId del base en BGG; {@code null} si el registro no lo tiene,
 *                   en cuyo caso no se puede consultar el catálogo
 */
public record BoardGameExpansionCatalog(
        String baseGameId,
        String baseBggId,
        List<BoardGameExpansion> expansions) {
}
