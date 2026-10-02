package com.wikicollection.infrastructure.adapter.in.web.dto;

import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Una impresión de carta para que el usuario elija cuál guardar.
 *
 * <p>El campo clave es {@code scryfallId}: es lo que se pasa a
 * {@code POST /api/v1/magic/scryfall/{scryfallId}}, y cada impresión tiene el suyo. Si se
 * devolviera el id de la carta en lugar del de la impresión, el usuario acabaría
 * guardando siempre la misma reimpresión.
 *
 * <p>{@code fullArt} es primitivo a propósito: Scryfall omite {@code full_art} en las
 * cartas antiguas y para el cliente "sin arte completo" es lo mismo que {@code false}.
 */
@Schema(name = "MagicCardPrintingResponse", description = "Impresión concreta de una carta de Scryfall")
public record MagicCardPrintingResponse(
        @Schema(description = "Identificador de esta impresión; el que se puede pasar a POST /api/v1/magic/scryfall/{id}")
        String scryfallId,
        @Schema(description = "Nombre de la carta") String name,
        @Schema(description = "Código de la expansión") String set,
        @Schema(description = "Nombre de la expansión") String setName,
        @Schema(description = "Número decollector dentro de la expansión") String collectorNumber,
        @Schema(description = "Rareza") String rarity,
        @Schema(description = "Artista") String artist,
        @Schema(description = "Fecha de lanzamiento (AAAA-MM-DD)") String releasedAt,
        @Schema(description = "Idioma de esta impresión") String lang,
        @Schema(description = "Imagen completa de la carta") String imageUrl,
        @Schema(description = "Recorte del arte") String artCropUrl,
        @Schema(description = "Acabados disponibles: nonfoil, foil") List<String> finishes,
        @Schema(description = "Si la impresión es de arte completo") boolean fullArt,
        @Schema(description = "Tipos de promoción") List<String> promoTypes,
        @Schema(description = "Efectos del marco") List<String> frameEffects,
        @Schema(description = "Color del marco") String borderColor,
        @Schema(description = "Precio en USD") String priceUsd,
        @Schema(description = "Precio en EUR") String priceEur) {
}
