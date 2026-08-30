package com.wikicollection.dto;

import java.util.List;

public record GoogleBookResult(
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