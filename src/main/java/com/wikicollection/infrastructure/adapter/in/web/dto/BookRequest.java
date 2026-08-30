package com.wikicollection.infrastructure.adapter.in.web.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import com.wikicollection.domain.model.BookStatus;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

public record BookRequest(
        @NotBlank(message = "El título es obligatorio") String title,
        @NotEmpty(message = "Debe contener al menos un autor") List<String> authors,
        String isbn,
        String publisher,
        LocalDate publishedDate,
        String description,
        @Min(value = 0, message = "El número de páginas no puede ser negativo") Integer pageCount,
        List<String> categories,
        String coverImage,
        String language,
        @NotNull(message = "El estado es obligatorio") BookStatus status,
        @Min(value = 1, message = "La valoración mínima es 1")
        @Max(value = 5, message = "La valoración máxima es 5") Integer userRating,
        String notes,
        List<String> tags,
        LocalDateTime dateCompleted,
        String externalSource,
        String externalId) {
}