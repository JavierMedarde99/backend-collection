package com.wikicollection.infrastructure.adapter.in.web.dto;

import java.util.List;

import jakarta.validation.constraints.NotBlank;

public record DeckRequest(
        @NotBlank(message = "El nombre del mazo es obligatorio") String name,
        String description,
        String commander,
        List<String> commanderColors) {
}
