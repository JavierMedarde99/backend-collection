package com.wikicollection.domain.model;

import java.util.List;

public record BookSearchCriteria(
        String name,
        String author,
        BookType type,
        BookState state,
        java.util.List<String> genres,
        String ownerId,
        List<String> excludeOwnerIds) {

    public boolean hasName() {
        return name != null && !name.isBlank();
    }

    public boolean hasAuthor() {
        return author != null && !author.isBlank();
    }

    public boolean hasGenres() {
        return genres != null && genres.stream().anyMatch(g -> g != null && !g.isBlank());
    }

    public java.util.List<String> effectiveGenres() {
        if (genres == null) {
            return java.util.List.of();
        }
        return genres.stream().filter(g -> g != null && !g.isBlank()).toList();
    }

    public boolean hasOwnerId() {
        return ownerId != null && !ownerId.isBlank();
    }

    public boolean hasExcludeOwnerIds() {
        return excludeOwnerIds != null && !excludeOwnerIds.isEmpty();
    }
}
