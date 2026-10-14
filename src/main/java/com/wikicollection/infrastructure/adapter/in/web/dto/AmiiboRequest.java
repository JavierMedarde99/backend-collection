package com.wikicollection.infrastructure.adapter.in.web.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import com.wikicollection.domain.model.AmiiboReleaseDates;
import com.wikicollection.domain.model.AmiiboStatus;
import com.wikicollection.domain.model.AmiiboType;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record AmiiboRequest(
        @NotBlank(message = "El externalId es obligatorio") String externalId,
        @NotBlank(message = "El nombre es obligatorio") String name,
        String character,
        String amiiboSeries,
        String gameSeries,
        AmiiboType amiiboType,
        AmiiboReleaseDates releaseDates,
        String imageUrl,
        List<String> compatibleGames,
        @NotNull(message = "El estado es obligatorio") AmiiboStatus status,
        String notes,
        LocalDate dateAdded,
        LocalDate acquisitionDate,
        BigDecimal acquisitionPrice) {
}
