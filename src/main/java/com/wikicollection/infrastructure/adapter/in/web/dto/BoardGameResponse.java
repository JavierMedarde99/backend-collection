package com.wikicollection.infrastructure.adapter.in.web.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import com.wikicollection.domain.model.BoardGameStatus;
import com.wikicollection.domain.model.Difficulty;

public record BoardGameResponse(
        String id,
        String title,
        List<String> genres,
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
        LocalDate dateAdded,
        Integer personalRating,
        Integer playCount,
        LocalDate lastPlayedDate,
        Difficulty difficulty,
        UserOwnedResponse userOwned) {


    public BoardGameResponse withoutPrivate() {
        return new BoardGameResponse(id, title, genres, description, yearPublished, minPlayers, maxPlayers,
                minPlaytime, maxPlaytime, publisher, designers, categories, mechanics, imageUrl,
                thumbnailUrl, bggRating, bggId, status, null, dateAdded, personalRating, playCount, lastPlayedDate, difficulty, userOwned);
    }
}
