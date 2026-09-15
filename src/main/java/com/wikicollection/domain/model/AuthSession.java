package com.wikicollection.domain.model;

public record AuthSession(
        User user,
        AuthTokens tokens) {
}
