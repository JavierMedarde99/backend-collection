package com.wikicollection.infrastructure.adapter.in.web.dto;

public record FunkoSearchResponse(
        String externalId,
        String name,
        String series,
        String number,
        String category,
        String exclusive,
        Boolean vaulted,
        Integer releaseYear,
        String franchise,
        String popType,
        Integer rarityScore,
        String imageUrl,
        String externalSource) {
}
