package com.wikicollection.infrastructure.adapter.in.web.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import com.wikicollection.domain.model.GamePlatform;
import com.wikicollection.domain.model.GameStatus;

public record GameResponse(
        String id,
        String externalId,
        String title,
        List<String> genres,
        GamePlatform platform,
        String thumbnailUrl,
        GameStatus status,
        Integer userRating,
        String comment,
        LocalDate dateAdded,
        LocalDate dateCompleted,
        String externalSource,
        String steamAppId,
        LocalDate acquisitionDate,
        BigDecimal acquisitionPrice,
        UserOwnedResponse userOwned) {


    /**
     * A diferencia de {@code BookResponse.withoutPrivate()}, aquí los datos de
     * adquisición se conservan: lo que se pagó y cuándo no se considera privado
     * en la colección de videojuegos, y así lo pide el frontend.
     */
    public GameResponse withoutPrivate() {
        return new GameResponse(id, externalId, title, genres, platform, thumbnailUrl, status,
                null, null, dateAdded, dateCompleted, externalSource, steamAppId,
                acquisitionDate, acquisitionPrice, userOwned);
    }
}
