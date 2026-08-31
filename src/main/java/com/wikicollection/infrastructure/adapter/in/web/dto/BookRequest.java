package com.wikicollection.infrastructure.adapter.in.web.dto;

import java.time.LocalDate;

import com.wikicollection.domain.model.BookState;
import com.wikicollection.domain.model.BookType;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record BookRequest(
        String externalId,
        @NotBlank(message = "El título es obligatorio") String title,
        String descripcion,
        @NotBlank(message = "El autor es obligatorio") String author,
        @Min(value = 0, message = "El número de páginas no puede ser negativo") Integer pages,
        @NotNull(message = "El tipo es obligatorio") BookType type,
        @NotNull(message = "El estado es obligatorio") BookState state,
        String comment,
        @Min(value = 0, message = "La puntuación mínima es 0")
        @Max(value = 5, message = "La puntuación máxima es 5") Integer start,
        LocalDate startDate,
        LocalDate endDate,
        String frontpage) {
}
