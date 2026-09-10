package com.wikicollection.infrastructure.adapter.in.web.dto;

import com.wikicollection.domain.model.DeckStatus;

public record DeckStatusResponse(
        DeckStatus status,
        String message) {
}
