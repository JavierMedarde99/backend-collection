package com.wikicollection.infrastructure.adapter.in.web.dto;

import java.util.List;

import com.wikicollection.domain.model.AmiiboReleaseDates;
import com.wikicollection.domain.model.AmiiboType;

public record AmiiboSearchResponse(
        String externalId,
        String name,
        String character,
        String amiiboSeries,
        String gameSeries,
        AmiiboType amiiboType,
        AmiiboReleaseDates releaseDates,
        String imageUrl,
        List<String> compatibleGames,
        String externalSource) {
}
