package com.wikicollection.infrastructure.adapter.in.web.dto;

import java.util.List;

import com.wikicollection.domain.model.GameStatus;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Body de {@code POST /games/{id}/additions}: los externalId de RAWG a dar de
 * alta como contenido adicional del juego base y el estado en el que nacen los
 * DLC nuevos. El límite de 40 es el de la página de adiciones de RAWG.
 */
public record AddGameAdditionsRequest(
        @NotEmpty(message = "Debes indicar al menos una adición")
        @Size(max = 40, message = "No se pueden añadir más de 40 adiciones por petición")
        List<@NotBlank(message = "El externalId no puede estar en blanco") String> externalIds,
        @NotNull(message = "El estado es obligatorio") GameStatus status) {
}
