package com.wikicollection.domain.model;

public record MovieSearchCriteria(
        String name,
        MovieStatus status,
        MovieMediaType mediaType) {

    public boolean hasName() {
        return name != null && !name.isBlank();
    }
}
