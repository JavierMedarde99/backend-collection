package com.wikicollection.infrastructure.adapter.in.web.dto;

public record AuthResponse(
        String accessToken,
        String refreshToken,
        UserResponse user) {
}
