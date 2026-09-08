package com.wikicollection.domain.model;

public record BoardGameSearchCriteria(
        String name,
        BoardGameStatus status) {

    public boolean hasName() {
        return name != null && !name.isBlank();
    }
}
