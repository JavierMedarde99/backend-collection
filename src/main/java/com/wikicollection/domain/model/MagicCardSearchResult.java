package com.wikicollection.domain.model;

import java.util.List;

public record MagicCardSearchResult(
        String scryfallId,
        String name,
        String manaCost,
        String type,
        String rarity,
        String setCode,
        String setName,
        String imageUrl,
        String priceUsd) {
}
