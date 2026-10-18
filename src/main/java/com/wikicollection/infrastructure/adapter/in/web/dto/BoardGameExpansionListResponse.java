package com.wikicollection.infrastructure.adapter.in.web.dto;

import java.util.List;

import com.wikicollection.domain.model.BoardGameExpansionCatalog;

public record BoardGameExpansionListResponse(
        String baseGameId,
        String baseBggId,
        List<BoardGameExpansionResponse> expansions) {

    public static BoardGameExpansionListResponse from(BoardGameExpansionCatalog catalog) {
        if (catalog == null) {
            return null;
        }
        return new BoardGameExpansionListResponse(
                catalog.baseGameId(),
                catalog.baseBggId(),
                catalog.expansions().stream().map(BoardGameExpansionResponse::from).toList());
    }
}
