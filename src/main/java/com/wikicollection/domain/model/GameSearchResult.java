package com.wikicollection.domain.model;

import java.time.LocalDate;

public record GameSearchResult(
        String id,
        String title,
        String description,
        String genre,
        String platform,
        String publisher,
        String developer,
        LocalDate releaseDate,
        String thumbnailUrl,
        String externalSource) {
}