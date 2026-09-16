package com.wikicollection.infrastructure.adapter.in.web.dto;

import java.util.Map;

import jakarta.validation.constraints.NotNull;

public record CollectionVisibilityRequest(
        @NotNull(message = "visibility es obligatorio") Map<String, String> visibility) {
}
