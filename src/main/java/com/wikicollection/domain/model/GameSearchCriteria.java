package com.wikicollection.domain.model;

public record GameSearchCriteria(
        String name,
        GamePlatform platform,
        GameStatus status) {

    public boolean hasName() {
        return name != null && !name.isBlank();
    }
}