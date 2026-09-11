package com.wikicollection.infrastructure.adapter.in.web.dto;

import java.time.LocalDate;

import com.wikicollection.domain.model.MovieMediaType;
import com.wikicollection.domain.model.MovieStatus;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

public record MovieShowRequest(
        @NotBlank(message = "El identificador externo es obligatorio") String externalId,
        @NotBlank(message = "El título es obligatorio") String title,
        String overview,
        LocalDate releaseDate,
        String posterUrl,
        String backdropUrl,
        Double voteAverage,
        MovieMediaType mediaType,
        MovieStatus status,
        @Min(value = 1, message = "La puntuación mínima es 1")
        @Max(value = 5, message = "La puntuación máxima es 5") Integer userRating,
        String comment,
        LocalDate dateAdded,
        LocalDate dateCompleted,
        String externalSource) {
}
