package com.wikicollection.infrastructure.adapter.in.web.dto;

import java.time.LocalDate;

import com.wikicollection.domain.model.MovieMediaType;
import com.wikicollection.domain.model.MovieStatus;

public record MovieShowResponse(
        String id,
        String externalId,
        String title,
        String overview,
        LocalDate releaseDate,
        String posterUrl,
        String backdropUrl,
        Double voteAverage,
        MovieMediaType mediaType,
        MovieStatus status,
        Integer userRating,
        String comment,
        LocalDate dateAdded,
        LocalDate dateCompleted,
        String externalSource) {
}
