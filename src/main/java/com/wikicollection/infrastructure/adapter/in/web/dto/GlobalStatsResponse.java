package com.wikicollection.infrastructure.adapter.in.web.dto;

import java.util.Map;

public record GlobalStatsResponse(
        Map<String, Long> collections) {
}
