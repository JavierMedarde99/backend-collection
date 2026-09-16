package com.wikicollection.infrastructure.adapter.in.web.dto;

import java.util.Map;

import jakarta.validation.constraints.NotNull;

public record UserPreferencesRequest(
        @NotNull(message = "activeCollections es obligatorio") Map<String, Boolean> activeCollections,
        @NotNull(message = "collectionVisibility es obligatorio") Map<String, String> collectionVisibility) {
}
