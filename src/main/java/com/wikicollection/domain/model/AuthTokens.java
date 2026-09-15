package com.wikicollection.domain.model;

public record AuthTokens(
        String accessToken,
        String refreshToken) {
}
