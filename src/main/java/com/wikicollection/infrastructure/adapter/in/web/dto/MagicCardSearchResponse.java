package com.wikicollection.infrastructure.adapter.in.web.dto;

import java.util.List;

import com.wikicollection.domain.model.MagicCardSearchResult;

public record MagicCardSearchResponse(
        String query,
        List<MagicCardSearchResult> results) {
}
