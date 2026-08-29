package com.wikicollection.infrastructure.adapter.in.web.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import com.wikicollection.domain.model.BookStatus;

public record BookResponse(
        String id,
        String title,
        List<String> authors,
        String isbn,
        String publisher,
        LocalDate publishedDate,
        String description,
        Integer pageCount,
        List<String> categories,
        String coverImage,
        String language,
        BookStatus status,
        Integer userRating,
        String notes,
        List<String> tags,
        LocalDateTime dateAdded,
        LocalDateTime dateCompleted,
        LocalDateTime dateUpdated,
        String externalSource,
        String externalId) {
}