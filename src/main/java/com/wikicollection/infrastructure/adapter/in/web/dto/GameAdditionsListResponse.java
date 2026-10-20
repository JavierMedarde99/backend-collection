package com.wikicollection.infrastructure.adapter.in.web.dto;

import java.util.List;

import com.wikicollection.domain.model.GameAdditionsCatalog;

public record GameAdditionsListResponse(
        String baseGameId,
        String baseExternalId,
        List<GameAdditionResponse> additions,
        int totalAvailable) {

    public static GameAdditionsListResponse from(GameAdditionsCatalog catalog) {
        if (catalog == null) {
            return null;
        }
        return new GameAdditionsListResponse(
                catalog.baseGameId(),
                catalog.baseExternalId(),
                catalog.additions().stream().map(GameAdditionResponse::from).toList(),
                catalog.totalAvailable());
    }
}
