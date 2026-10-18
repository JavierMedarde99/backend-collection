package com.wikicollection.infrastructure.adapter.in.web.dto;

import com.wikicollection.domain.model.BoardGameExpansionSkip;

/**
 * Un bggId pedido que no se ha creado porque ya estaba en la colección.
 */
public record SkippedExpansionResponse(String bggId, String reason) {

    public static SkippedExpansionResponse from(BoardGameExpansionSkip skip) {
        if (skip == null) {
            return null;
        }
        return new SkippedExpansionResponse(skip.bggId(), skip.reason());
    }
}
