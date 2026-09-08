package com.wikicollection.domain.model;

public record MagicCardSearchCriteria(
        String name) {

    public boolean hasName() {
        return name != null && !name.isBlank();
    }
}
