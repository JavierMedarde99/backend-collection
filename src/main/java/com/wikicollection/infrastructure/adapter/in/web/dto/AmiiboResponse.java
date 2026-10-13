package com.wikicollection.infrastructure.adapter.in.web.dto;

import java.time.LocalDate;
import java.util.List;

import com.wikicollection.domain.model.AmiiboReleaseDates;
import com.wikicollection.domain.model.AmiiboStatus;
import com.wikicollection.domain.model.AmiiboType;

public record AmiiboResponse(
        String id,
        String externalId,
        String name,
        String character,
        String amiiboSeries,
        String gameSeries,
        AmiiboType amiiboType,
        AmiiboReleaseDates releaseDates,
        String imageUrl,
        List<String> compatibleGames,
        AmiiboStatus status,
        String notes,
        LocalDate dateAdded,
        UserOwnedResponse userOwned) {

    public AmiiboResponse withoutPrivate() {
        return new AmiiboResponse(id, externalId, name, character, amiiboSeries, gameSeries, amiiboType,
                releaseDates, imageUrl, compatibleGames, status, null, dateAdded, userOwned);
    }
}
