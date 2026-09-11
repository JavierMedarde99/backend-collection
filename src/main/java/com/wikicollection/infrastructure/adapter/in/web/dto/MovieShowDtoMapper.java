package com.wikicollection.infrastructure.adapter.in.web.dto;

import com.wikicollection.domain.model.MovieShow;

import org.springframework.stereotype.Component;

@Component
public class MovieShowDtoMapper {

    public MovieShow toDomain(MovieShowRequest request) {
        if (request == null) {
            return null;
        }
        return MovieShow.builder()
                .externalId(request.externalId())
                .title(request.title())
                .overview(request.overview())
                .releaseDate(request.releaseDate())
                .posterUrl(request.posterUrl())
                .backdropUrl(request.backdropUrl())
                .voteAverage(request.voteAverage())
                .mediaType(request.mediaType())
                .status(request.status())
                .userRating(request.userRating())
                .comment(request.comment())
                .dateAdded(request.dateAdded())
                .dateCompleted(request.dateCompleted())
                .externalSource(request.externalSource())
                .build();
    }

    public MovieShowResponse toResponse(MovieShow movieShow) {
        if (movieShow == null) {
            return null;
        }
        return new MovieShowResponse(
                movieShow.getId(),
                movieShow.getExternalId(),
                movieShow.getTitle(),
                movieShow.getOverview(),
                movieShow.getReleaseDate(),
                movieShow.getPosterUrl(),
                movieShow.getBackdropUrl(),
                movieShow.getVoteAverage(),
                movieShow.getMediaType(),
                movieShow.getStatus(),
                movieShow.getUserRating(),
                movieShow.getComment(),
                movieShow.getDateAdded(),
                movieShow.getDateCompleted(),
                movieShow.getExternalSource());
    }
}
