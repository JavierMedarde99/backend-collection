package com.wikicollection.infrastructure.adapter.in.web.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;

import com.wikicollection.domain.model.GamePlatform;
import com.wikicollection.domain.model.GameStatus;

public record GameResponse(
        String id,
        String externalId,
        String title,
        String description,
        String genre,
        GamePlatform platform,
        String publisher,
        String developer,
        LocalDate releaseDate,
        String thumbnailUrl,
        GameStatus status,
        Integer userRating,
        String notes,
        LocalDateTime dateAdded,
        LocalDateTime dateCompleted,
        String externalSource) {
}