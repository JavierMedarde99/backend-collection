package com.wikicollection.infrastructure.adapter.in.web.dto;

import java.time.LocalDateTime;
import java.util.List;

public record DeckResponse(
        String id,
        String name,
        String description,
        String commander,
        List<String> commanderColors,
        List<DeckCardResponse> cards,
        LocalDateTime createdAt,
        LocalDateTime updatedAt) {
}
