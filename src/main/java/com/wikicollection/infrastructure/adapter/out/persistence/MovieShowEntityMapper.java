package com.wikicollection.infrastructure.adapter.out.persistence;

import java.util.List;

import com.wikicollection.domain.model.MovieShow;
import com.wikicollection.domain.model.ProviderAccessType;
import com.wikicollection.domain.model.StreamingProvider;

import org.springframework.stereotype.Component;

@Component
public class MovieShowEntityMapper {

    public MovieShowEntity toEntity(MovieShow movieShow) {
        if (movieShow == null) {
            return null;
        }
        return MovieShowEntity.builder()
                .id(movieShow.getId())
                .ownerId(movieShow.getOwnerId())
                .userOwned(UserOwnedMapping.toEntity(movieShow.getUserOwned()))
                .externalId(movieShow.getExternalId())
                .title(movieShow.getTitle())
                .genres(movieShow.getGenres())
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
                .streamingProviders(toEntities(movieShow.getStreamingProviders()))
                .watchCountry(movieShow.getWatchCountry())
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
                .ownerId(entity.getOwnerId())
                .userOwned(UserOwnedMapping.toDomain(entity.getUserOwned()))
                .externalId(entity.getExternalId())
                .title(entity.getTitle())
                .genres(entity.getGenres())
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
                .streamingProviders(toDomainList(entity.getStreamingProviders()))
                .watchCountry(entity.getWatchCountry())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }

    private static List<StreamingProviderEntity> toEntities(List<StreamingProvider> providers) {
        if (providers == null) {
            return null;
        }
        return providers.stream()
                .map(p -> StreamingProviderEntity.builder()
                        .providerId(p.getProviderId())
                        .providerName(p.getProviderName())
                        .logoUrl(p.getLogoUrl())
                        .type(p.getType() == null ? null : p.getType().name())
                        .deepLinkUrl(p.getDeepLinkUrl())
                        .build())
                .toList();
    }

    private static List<StreamingProvider> toDomainList(List<StreamingProviderEntity> entities) {
        if (entities == null) {
            return null;
        }
        return entities.stream()
                .map(e -> StreamingProvider.builder()
                        .providerId(e.getProviderId())
                        .providerName(e.getProviderName())
                        .logoUrl(e.getLogoUrl())
                        .type(parseType(e.getType()))
                        .deepLinkUrl(e.getDeepLinkUrl())
                        .build())
                .toList();
    }

    private static ProviderAccessType parseType(String type) {
        if (type == null) {
            return null;
        }
        try {
            return ProviderAccessType.valueOf(type);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
