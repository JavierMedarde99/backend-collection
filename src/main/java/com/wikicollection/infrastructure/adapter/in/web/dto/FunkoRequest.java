package com.wikicollection.infrastructure.adapter.in.web.dto;

import java.time.LocalDate;

import com.wikicollection.domain.model.FunkoStatus;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record FunkoRequest(
        @NotBlank(message = "El externalId es obligatorio") String externalId,
        @NotBlank(message = "El nombre es obligatorio") String name,
        String series,
        String number,
        String category,
        String exclusive,
        Boolean vaulted,
        Integer releaseYear,
        String franchise,
        String popType,
        Integer rarityScore,
        String imageUrl,
        @NotNull(message = "El estado es obligatorio") FunkoStatus status,
        String notes,
        LocalDate dateAdded) {
}
