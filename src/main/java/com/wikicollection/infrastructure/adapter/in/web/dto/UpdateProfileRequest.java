package com.wikicollection.infrastructure.adapter.in.web.dto;

import jakarta.validation.constraints.Size;

public record UpdateProfileRequest(
        @Size(max = 100, message = "El nombre visible no puede superar los 100 caracteres")
        String displayName,
        @Size(max = 500, message = "El avatar no puede superar los 500 caracteres")
        String avatarUrl,
        @Size(max = 500, message = "La bio no puede superar los 500 caracteres")
        String bio) {
}
