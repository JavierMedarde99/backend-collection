package com.wikicollection.domain.model;

import java.time.LocalDate;

public record MovieSearchResult(
        String externalId,
        String title,
        String overview,
        LocalDate releaseDate,
        String posterUrl,
        String backdropUrl,
        Double voteAverage,
        MovieMediaType mediaType,
        String externalSource) {
}
