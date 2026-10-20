package com.wikicollection.domain.model;

import java.util.List;

/**
 * Catálogo de adiciones que el catálogo externo cuelga de un juego base.
 *
 * @param baseGameId    id local del juego base en la colección
 * @param baseExternalId externalId del base en RAWG
 * @param additions     primera página de adiciones, ya con los flags del espectador
 * @param totalAvailable cuántas adiciones conoce el catálogo en total, no solo las de esta página
 */
public record GameAdditionsCatalog(
        String baseGameId,
        String baseExternalId,
        List<GameAddition> additions,
        int totalAvailable) {
}
