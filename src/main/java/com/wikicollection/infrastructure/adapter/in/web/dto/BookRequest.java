package com.wikicollection.infrastructure.adapter.in.web.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import com.wikicollection.domain.model.BookState;
import com.wikicollection.domain.model.BookType;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record BookRequest(
        String externalId,
        String isbn,
        @NotBlank(message = "El título es obligatorio") String title,
        String descripcion,
        @NotBlank(message = "El autor es obligatorio") String author,
        List<String> genres,
        @Min(value = 0, message = "El número de páginas no puede ser negativo") Integer pages,
        @NotNull(message = "El tipo es obligatorio") BookType type,
        @NotNull(message = "El estado es obligatorio") BookState state,
        String comment,
        @Min(value = 0, message = "La puntuación mínima es 0")
        @Max(value = 5, message = "La puntuación máxima es 5") Integer start,
        @Min(value = 0, message = "Las páginas leídas no pueden ser negativas") Integer pagesRead,
        LocalDate startDate,
        LocalDate endDate,
        String frontpage,
        String publisher,
        @Min(value = 1, message = "El año de publicación no puede ser anterior a 1")
        @Max(value = 9999, message = "El año de publicación no puede ser posterior a 9999") Integer publicationYear,
        LocalDate acquisitionDate,
        @Min(value = 0, message = "El precio de adquisición no puede ser negativo")
        @Digits(integer = 8, fraction = 2, message = "El precio admite como máximo 8 dígitos y 2 decimales")
        BigDecimal acquisitionPrice) {
}
