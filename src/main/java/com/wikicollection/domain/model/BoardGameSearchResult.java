package com.wikicollection.domain.model;

import java.math.BigDecimal;

public record BoardGameSearchResult(
        String bggId,
        String title,
        String description,
        Integer yearPublished,
        Integer minPlayers,
        Integer maxPlayers,
        Integer minPlaytime,
        Integer maxPlaytime,
        String publisher,
        java.util.List<String> designers,
        java.util.List<String> categories,
        java.util.List<String> mechanics,
        String imageUrl,
        String thumbnailUrl,
        BigDecimal bggRating,
        String externalSource) {
}
