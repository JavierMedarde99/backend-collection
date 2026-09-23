package com.wikicollection.infrastructure.adapter.in.web.dto;

import java.time.LocalDate;
import java.util.List;

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
        String externalSource,
        List<StreamingProviderResponse> streamingProviders,
        String watchCountry,
        UserOwnedResponse userOwned) {


    public MovieShowResponse withoutPrivate() {
        return new MovieShowResponse(id, externalId, title, overview, releaseDate, posterUrl,
                backdropUrl, voteAverage, mediaType, status, null, null, dateAdded, dateCompleted,
                externalSource, streamingProviders, watchCountry, userOwned);
    }
}
