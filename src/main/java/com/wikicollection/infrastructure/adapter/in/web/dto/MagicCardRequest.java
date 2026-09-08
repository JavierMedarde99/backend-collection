package com.wikicollection.infrastructure.adapter.in.web.dto;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import com.wikicollection.domain.model.MagicCardCondition;
import com.wikicollection.domain.model.MagicCardLanguage;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

public record MagicCardRequest(
        @NotBlank(message = "El nombre es obligatorio") String name,
        MagicCardLanguage language,
        String releaseDate,
        String manaCost,
        Double convertedManaCost,
        String type,
        String text,
        String power,
        String toughness,
        String loyalty,
        List<String> colors,
        List<String> colorIdentity,
        List<String> keywords,
        String rarity,
        String setCode,
        String setName,
        String artist,
        String frame,
        String borderColor,
        String layout,
        Map<String, String> legalities,
        String priceUsd,
        String priceEur,
        String imageUrl,
        String imageLargeUrl,
        String artCropUrl,
        MagicCardCondition condition,
        Boolean isFoil,
        @Min(value = 1, message = "La cantidad mínima es 1") Integer quantity,
        String notes) {
}
