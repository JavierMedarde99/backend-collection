package com.wikicollection.infrastructure.adapter.in.web.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

public record DeckCardRequest(
        @NotBlank(message = "El identificador Scryfall es obligatorio") String scryfallId,
        @Min(value = 1, message = "La cantidad mínima es 1") Integer quantity) {
}
