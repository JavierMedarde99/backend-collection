package com.wikicollection.infrastructure.adapter.in.web.dto;

import java.util.List;
import java.util.function.Function;

import com.wikicollection.domain.model.Game;
import com.wikicollection.domain.model.GameAdditionsAddResult;

/**
 * Respuesta de {@code POST /games/{id}/additions}: los DLC creados, los juegos
 * sueltos enlazados al base y los que ya colgaban de él.
 */
public record GameAdditionsAddResponse(
        List<GameResponse> created,
        List<GameResponse> linked,
        List<GameAdditionSkipResponse> skipped) {

    public static GameAdditionsAddResponse from(GameAdditionsAddResult result,
            Function<Game, GameResponse> mapper) {
        if (result == null) {
            return null;
        }
        return new GameAdditionsAddResponse(
                result.created().stream().map(mapper).toList(),
                result.linked().stream().map(mapper).toList(),
                result.skipped().stream().map(GameAdditionSkipResponse::from).toList());
    }
}
