package com.wikicollection.domain.model;

import java.util.List;

public record BookSearchResult(
        String id,
        String title,
        List<String> authors,
        String isbn,
        String coverImage,
        String description,
        Integer pageCount,
        String publisher,
        String publishedDate,
        String language,
        List<String> categories) {
}