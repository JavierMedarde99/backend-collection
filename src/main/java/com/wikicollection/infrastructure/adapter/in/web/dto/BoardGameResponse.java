package com.wikicollection.infrastructure.adapter.in.web.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import com.wikicollection.domain.model.BoardGameStatus;

public record BoardGameResponse(
        String id,
        String title,
        String description,
        Integer yearPublished,
        Integer minPlayers,
        Integer maxPlayers,
        Integer minPlaytime,
        Integer maxPlaytime,
        String publisher,
        List<String> designers,
        List<String> categories,
        List<String> mechanics,
        String imageUrl,
        String thumbnailUrl,
        BigDecimal bggRating,
        String bggId,
        BoardGameStatus status,
        String notes,
        LocalDate dateAdded) {
}