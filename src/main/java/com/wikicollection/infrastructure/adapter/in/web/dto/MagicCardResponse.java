package com.wikicollection.infrastructure.adapter.in.web.dto;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import com.wikicollection.domain.model.MagicCardCondition;
import com.wikicollection.domain.model.MagicCardLanguage;

public record MagicCardResponse(
        String id,
        String scryfallId,
        String oracleId,
        String name,
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
        Integer quantity,
        String notes,
        LocalDateTime dateAdded) {
}
