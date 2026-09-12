package com.wikicollection.infrastructure.adapter.out.persistence;

import com.wikicollection.domain.model.MovieShow;

import org.springframework.stereotype.Component;

@Component
public class MovieShowEntityMapper {

    public MovieShowEntity toEntity(MovieShow movieShow) {
        if (movieShow == null) {
            return null;
        }
        return MovieShowEntity.builder()
                .id(movieShow.getId())
                .externalId(movieShow.getExternalId())
                .title(movieShow.getTitle())
                .overview(movieShow.getOverview())
                .releaseDate(movieShow.getReleaseDate())
                .posterUrl(movieShow.getPosterUrl())
                .backdropUrl(movieShow.getBackdropUrl())
                .voteAverage(movieShow.getVoteAverage())
                .mediaType(movieShow.getMediaType())
                .status(movieShow.getStatus())
                .userRating(movieShow.getUserRating())
                .comment(movieShow.getComment())
                .dateAdded(movieShow.getDateAdded())
                .dateCompleted(movieShow.getDateCompleted())
                .externalSource(movieShow.getExternalSource())
                .createdAt(movieShow.getCreatedAt())
                .updatedAt(movieShow.getUpdatedAt())
                .build();
    }

    public MovieShow toDomain(MovieShowEntity entity) {
        if (entity == null) {
            return null;
        }
        return MovieShow.builder()
                .id(entity.getId())
                .externalId(entity.getExternalId())
                .title(entity.getTitle())
                .overview(entity.getOverview())
                .releaseDate(entity.getReleaseDate())
                .posterUrl(entity.getPosterUrl())
                .backdropUrl(entity.getBackdropUrl())
                .voteAverage(entity.getVoteAverage())
                .mediaType(entity.getMediaType())
                .status(entity.getStatus())
                .userRating(entity.getUserRating())
                .comment(entity.getComment())
                .dateAdded(entity.getDateAdded())
                .dateCompleted(entity.getDateCompleted())
                .externalSource(entity.getExternalSource())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }
}
