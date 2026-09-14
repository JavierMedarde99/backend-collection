package com.wikicollection.infrastructure.adapter.in.web.dto;

import org.springframework.data.domain.Page;

import com.wikicollection.domain.model.MagicCardSearchResult;

public record MagicCardSearchResponse(
        String query,
        Page<MagicCardSearchResult> results) {
}
