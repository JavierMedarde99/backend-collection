package com.wikicollection.infrastructure.adapter.in.web.dto;

import java.time.LocalDate;

import com.wikicollection.domain.model.GamePlatform;
import com.wikicollection.domain.model.GameStatus;

public record GameResponse(
        String id,
        String externalId,
        String title,
        GamePlatform platform,
        String thumbnailUrl,
        GameStatus status,
        Integer userRating,
        String comment,
        LocalDate dateAdded,
        LocalDate dateCompleted,
        String externalSource) {
}