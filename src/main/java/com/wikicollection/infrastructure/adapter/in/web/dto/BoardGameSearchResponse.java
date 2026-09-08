package com.wikicollection.infrastructure.adapter.in.web.dto;

import java.util.List;

import com.wikicollection.domain.model.BoardGameSearchResult;

public record BoardGameSearchResponse(
        String query,
        List<BoardGameSearchResult> results) {
}