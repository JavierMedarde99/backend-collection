package com.wikicollection.domain.model;

import java.time.LocalDate;
import java.util.List;

/**
 * Una adición del catálogo externo de un juego base (DLC, expansión, edición
 * especial...) tal y como se pinta en el diálogo de altas.
 *
 * <p>Los dos flags dependen del espectador y se calculan fuera de la caché del
 * catálogo: el mismo juego de RAWG se ve distinto para cada usuario.
 *
 * @param inCollection  el usuario ya tiene un juego con ese {@code externalId}
 * @param alreadyLinked ese juego además cuelga de este mismo base
 */
public record GameAddition(
        String externalId,
        String title,
        String platform,
        List<String> genres,
        String thumbnailUrl,
        LocalDate releaseDate,
        String externalSource,
        boolean inCollection,
        boolean alreadyLinked) {
}
