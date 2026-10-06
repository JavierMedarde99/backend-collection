package com.wikicollection.infrastructure.adapter.in.web.dto;

import java.util.List;

/**
 * Una carta que Scryfall propuso para un nombre que no se pudo resolver (#353).
 *
 * <p>Es lo que el frontend necesita pintar para que el usuario elija, en lugar de guessing
 * otra vez por su cuenta.
 */
public record DeckImportCandidateResponse(
        String scryfallId,
        String name,
        String manaCost,
        String type,
        String rarity,
        String setCode,
        String setName,
        String imageUrl,
        String priceUsd,
        List<String> colorIdentity) {
}