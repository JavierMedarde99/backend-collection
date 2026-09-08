package com.wikicollection.infrastructure.adapter.in.web.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import com.wikicollection.domain.model.BoardGameStatus;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record BoardGameRequest(
        @NotBlank(message = "El título es obligatorio") String title,
        String description,
        Integer yearPublished,
        @Min(value = 1, message = "El número mínimo de jugadores es 1") Integer minPlayers,
        @Min(value = 1, message = "El número máximo de jugadores es 1") Integer maxPlayers,
        @Min(value = 1, message = "El tiempo de juego mínimo es 1") Integer minPlaytime,
        @Min(value = 1, message = "El tiempo de juego máximo es 1") Integer maxPlaytime,
        String publisher,
        List<String> designers,
        List<String> categories,
        List<String> mechanics,
        String imageUrl,
        String thumbnailUrl,
        @DecimalMin(value = "0.0", message = "La valoración mínima es 0")
        @DecimalMax(value = "10.0", message = "La valoración máxima es 10") BigDecimal bggRating,
        String bggId,
        @NotNull(message = "El estado es obligatorio") BoardGameStatus status,
        String notes,
        LocalDate dateAdded) {
}