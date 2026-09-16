package com.wikicollection.infrastructure.adapter.in.web.dto;

import java.util.Map;

import jakarta.validation.constraints.NotNull;

public record ActiveCollectionsRequest(
        @NotNull(message = "collections es obligatorio") Map<String, Boolean> collections) {
}
