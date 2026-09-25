package com.wikicollection.domain.model;

import java.util.List;

public record BookSearchCriteria(
        String name,
        String author,
        BookType type,
        BookState state,
        String genre,
        String ownerId,
        List<String> excludeOwnerIds) {

    public boolean hasName() {
        return name != null && !name.isBlank();
    }

    public boolean hasAuthor() {
        return author != null && !author.isBlank();
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
