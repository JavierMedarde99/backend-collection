package com.wikicollection.infrastructure.adapter.in.web.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @NotBlank(message = "El username es obligatorio")
        @Size(min = 3, max = 20, message = "El username debe tener entre 3 y 20 caracteres")
        @Pattern(regexp = "^[a-zA-Z0-9_]+$", message = "El username solo admite letras, números y _")
        String username,
        @NotBlank(message = "El email es obligatorio")
        @Email(message = "Email inválido")
        String email,
        @NotBlank(message = "La contraseña es obligatoria")
        @Size(min = 8, message = "La contraseña debe tener al menos 8 caracteres")
        @Pattern(regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[^a-zA-Z0-9]).+$",
                message = "La contraseña debe incluir mayúscula, minúscula, número y carácter especial")
        String password,
        String displayName) {
}
