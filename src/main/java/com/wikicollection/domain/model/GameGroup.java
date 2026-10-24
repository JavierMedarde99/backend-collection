package com.wikicollection.domain.model;

import java.util.List;

/**
 * Un juego de la colección junto con todas sus filas por plataforma.
 *
 * <p>La colección guarda un registro plano por plataforma ({@code externalId, title, platform,
 * status, …}), así que el mismo juego añadido para PC y para Switch son dos documentos. El
 * listado con {@code groupBy=game} los presenta como una sola entrada.
 *
 * @param gameKey      clave de agrupación: {@code x:<externalId>} si el registro viene del catálogo
 *                     o {@code t:<título normalizado>} si es un alta manual
 * @param title        título del primer registro del grupo (plataforma ascendente)
 * @param thumbnailUrl carátula del primer registro del grupo
 * @param games        filas del grupo, ordenadas por plataforma ascendente
 */
public record GameGroup(
        String gameKey,
        String title,
        String thumbnailUrl,
        List<Game> games) {
}
