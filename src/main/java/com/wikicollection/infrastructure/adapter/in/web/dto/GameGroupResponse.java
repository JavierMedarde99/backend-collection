package com.wikicollection.infrastructure.adapter.in.web.dto;

import java.util.List;

public record GameGroupResponse(
        String gameKey,
        String title,
        String thumbnailUrl,
        List<GameResponse> games) {
}
