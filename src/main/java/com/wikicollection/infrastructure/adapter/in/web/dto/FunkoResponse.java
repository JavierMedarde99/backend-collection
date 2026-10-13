package com.wikicollection.infrastructure.adapter.in.web.dto;

import java.time.LocalDate;

import com.wikicollection.domain.model.FunkoStatus;

public record FunkoResponse(
        String id,
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
        FunkoStatus status,
        String notes,
        LocalDate dateAdded,
        UserOwnedResponse userOwned) {

    public FunkoResponse withoutPrivate() {
        return new FunkoResponse(id, externalId, name, series, number, category, exclusive, vaulted,
                releaseYear, franchise, popType, rarityScore, imageUrl, status, null, dateAdded, userOwned);
    }
}
