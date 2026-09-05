package com.wikicollection.infrastructure.adapter.in.web.dto;

import java.time.LocalDateTime;

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
        LocalDateTime dateAdded,
        LocalDateTime dateCompleted,
        String externalSource) {
}