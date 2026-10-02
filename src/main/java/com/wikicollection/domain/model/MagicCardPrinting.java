package com.wikicollection.domain.model;

import java.util.List;

/**
 * Una impresión concreta de una carta de Scryfall.
 *
 * <p>Scryfall usa dos identificadores: {@code id} identifica una impresión
 * concreta y {@code oracle_id} identifica la carta a través de todas sus
 * reimpresiones. Un mismo nombre puede tener cientos de impresiones (Llanuras
 * tiene 955), con distinto arte, marco, precio y finishes. Como
 * {@code DeckCard} guarda el {@code scryfallId}, en un mazo de comandante la
 * impresión exacta es parte del producto y no basta con devolver "la carta".
 *
 * @param scryfallId identificador de ESTA impresión; es el que se puede pasar a
 *                   {@code POST /api/v1/magic/scryfall/{scryfallId}}
 * @param fullArt    primitivo a propósito: Scryfall omite {@code full_art} en las
 *                   cartas antiguas, y para el frontend "sin arte completo" es lo
 *                   mismo que {@code false}, no un tercer estado
 */
public record MagicCardPrinting(
        String scryfallId,
        String name,
        String setCode,
        String setName,
        String collectorNumber,
        String rarity,
        String artist,
        String releasedAt,
        String lang,
        String imageUrl,
        String artCropUrl,
        List<String> finishes,
        boolean fullArt,
        List<String> promoTypes,
        List<String> frameEffects,
        String borderColor,
        String priceUsd,
        String priceEur) {

    /**
     * Scryfall ignora {@code page_size}, {@code per_page} y {@code limit}: una pagina de
     * impresiones trae siempre 175 elementos (medido sobre {@code is:commander}, 12760
     * impresiones, con y sin {@code page_size=1}). Vive en el dominio porque forma parte
     * del contrato que ve el cliente: el {@code size} de la respuesta es este numero.
     */
    public static final int PAGE_SIZE = 175;
}
