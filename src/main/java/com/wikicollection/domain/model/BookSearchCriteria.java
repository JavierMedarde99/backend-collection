package com.wikicollection.domain.model;

public record BookSearchCriteria(
        String name,
        String author,
        BookType type,
        BookState state) {

    public boolean hasName() {
        return name != null && !name.isBlank();
    }

    public boolean hasAuthor() {
        return author != null && !author.isBlank();
    }
}
