package com.wikicollection.infrastructure.adapter.in.web.dto;

import java.util.List;

import com.wikicollection.domain.model.BoardGameExpansionAddResult;

/**
 * Respuesta de {@code POST /boardgames/{id}/expansions}: lo creado y lo que ya
 * estaba en la colección (el alta es idempotente).
 */
public record AddExpansionsResponse(
        List<BoardGameResponse> created,
        List<SkippedExpansionResponse> skipped) {

    public static AddExpansionsResponse from(BoardGameExpansionAddResult result,
            java.util.function.Function<com.wikicollection.domain.model.BoardGame, BoardGameResponse> mapper) {
        if (result == null) {
            return null;
        }
        return new AddExpansionsResponse(
                result.created().stream().map(mapper).toList(),
                result.skipped().stream().map(SkippedExpansionResponse::from).toList());
    }
}
