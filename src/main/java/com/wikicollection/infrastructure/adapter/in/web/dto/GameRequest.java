package com.wikicollection.infrastructure.adapter.in.web.dto;

import java.time.LocalDateTime;

import com.wikicollection.domain.model.GamePlatform;
import com.wikicollection.domain.model.GameStatus;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record GameRequest(
        String externalId,
        @NotBlank(message = "El título es obligatorio") String title,
        @NotNull(message = "La plataforma es obligatoria") GamePlatform platform,
        String thumbnailUrl,
        @NotNull(message = "El estado es obligatorio") GameStatus status,
        @Min(value = 1, message = "La puntuación mínima es 1")
        @Max(value = 5, message = "La puntuación máxima es 5") Integer userRating,
        String comment,
        LocalDateTime dateAdded,
        LocalDateTime dateCompleted,
        String externalSource) {
}