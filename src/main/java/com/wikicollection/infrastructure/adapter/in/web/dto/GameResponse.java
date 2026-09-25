package com.wikicollection.infrastructure.adapter.in.web.dto;

import java.time.LocalDate;
import java.util.List;

import com.wikicollection.domain.model.GamePlatform;
import com.wikicollection.domain.model.GameStatus;

public record GameResponse(
        String id,
        String externalId,
        String title,
        List<String> genres,
        GamePlatform platform,
        String thumbnailUrl,
        GameStatus status,
        Integer userRating,
        String comment,
        LocalDate dateAdded,
        LocalDate dateCompleted,
        String externalSource,
        String steamAppId,
        UserOwnedResponse userOwned) {


    public GameResponse withoutPrivate() {
        return new GameResponse(id, externalId, title, genres, platform, thumbnailUrl, status,
                null, null, dateAdded, dateCompleted, externalSource, steamAppId, userOwned);
    }
}
