package com.wikicollection.infrastructure.adapter.in.web.dto;

import org.springframework.data.domain.Page;

import com.wikicollection.domain.model.BoardGameSearchResult;

public record BoardGameSearchResponse(
        String query,
        Page<BoardGameSearchResult> results) {
}