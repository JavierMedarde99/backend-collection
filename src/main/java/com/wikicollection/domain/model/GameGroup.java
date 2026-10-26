package com.wikicollection.domain.model;

import java.util.List;

/**
 * Un videojuego del catálogo con una fila por plataforma en la que está el mismo juego.
 *
 * <p>{@code gameKey} es {@code "x:" + externalId} cuando el registro viene del catálogo y
 * {@code "t:" + título normalizado} en las altas manuales. Las filas de {@link #games()} van
 * ordenadas por plataforma ascendente para que la primera sea un destino determinista.
 */
public record GameGroup(
        String gameKey,
        String title,
        String thumbnailUrl,
        List<Game> games) {
}
