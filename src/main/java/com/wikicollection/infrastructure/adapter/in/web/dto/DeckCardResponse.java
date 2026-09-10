package com.wikicollection.infrastructure.adapter.in.web.dto;

import java.util.List;

public record DeckCardResponse(
        String cardName,
        Integer quantity,
        Boolean inCollection,
        Boolean isProxy,
        String manaCost,
        String typeLine,
        List<String> colorIdentity,
        String imageUrl,
        String scryfallId) {
}
