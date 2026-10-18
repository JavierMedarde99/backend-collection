package com.wikicollection.infrastructure.adapter.in.web.dto;

import java.util.List;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

/**
 * Body de {@code POST /boardgames/{id}/expansions}: los bggId de las expansiones
 * de BGG a dar de alta. El límite de 20 es el de la propia API de BGG por llamada.
 */
public record AddExpansionsRequest(
        @NotEmpty(message = "Debes indicar al menos un bggId")
        @Size(max = 20, message = "No se pueden añadir más de 20 expansiones por petición")
        List<@NotBlank(message = "El bggId no puede estar en blanco") String> bggIds) {
}
