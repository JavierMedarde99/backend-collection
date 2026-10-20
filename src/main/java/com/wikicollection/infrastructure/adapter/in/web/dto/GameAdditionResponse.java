package com.wikicollection.infrastructure.adapter.in.web.dto;

import java.time.LocalDate;
import java.util.List;

import com.wikicollection.domain.model.GameAddition;

/**
 * Una adición del catálogo externo de un juego base. Los flags
 * {@code inCollection}/{@code alreadyLinked} los calcula el servicio con los
 * registros del usuario: no vienen del catálogo y por eso se cachea fuera.
 */
public record GameAdditionResponse(
        String externalId,
        String title,
        String platform,
        List<String> genres,
        String thumbnailUrl,
        LocalDate releaseDate,
        String externalSource,
        boolean inCollection,
        boolean alreadyLinked) {

    public static GameAdditionResponse from(GameAddition addition) {
        if (addition == null) {
            return null;
        }
        return new GameAdditionResponse(
                addition.externalId(),
                addition.title(),
                addition.platform(),
                addition.genres(),
                addition.thumbnailUrl(),
                addition.releaseDate(),
                addition.externalSource(),
                addition.inCollection(),
                addition.alreadyLinked());
    }
}
