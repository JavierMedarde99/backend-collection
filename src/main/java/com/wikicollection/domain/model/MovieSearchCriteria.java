package com.wikicollection.domain.model;

import java.util.List;

public record MovieSearchCriteria(
        String name,
        MovieStatus status,
        MovieMediaType mediaType,
        String genre,
        String ownerId,
        List<String> excludeOwnerIds) {

    public boolean hasName() {
        return name != null && !name.isBlank();
    }

    public boolean hasGenre() {
        return genre != null && !genre.isBlank();
    }

    public boolean hasOwnerId() {
        return ownerId != null && !ownerId.isBlank();
    }

    public boolean hasExcludeOwnerIds() {
        return excludeOwnerIds != null && !excludeOwnerIds.isEmpty();
    }
}
