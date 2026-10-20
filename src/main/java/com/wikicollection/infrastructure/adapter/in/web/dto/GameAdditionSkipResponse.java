package com.wikicollection.infrastructure.adapter.in.web.dto;

import com.wikicollection.domain.model.GameAdditionSkip;

/**
 * Un externalId pedido que no se ha creado porque ya colgaba de este mismo base.
 */
public record GameAdditionSkipResponse(String externalId, String reason) {

    public static GameAdditionSkipResponse from(GameAdditionSkip skip) {
        if (skip == null) {
            return null;
        }
        return new GameAdditionSkipResponse(skip.externalId(), skip.reason());
    }
}
