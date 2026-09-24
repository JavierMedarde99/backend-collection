package com.wikicollection.infrastructure.adapter.in.web.dto;

import java.util.List;

import com.wikicollection.domain.model.MovieShow;
import com.wikicollection.domain.model.ProviderAccessType;
import com.wikicollection.domain.model.StreamingProvider;

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
                .streamingProviders(toDomainProviders(request.streamingProviders()))
                .watchCountry(request.watchCountry())
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
                movieShow.getExternalSource(),
                toResponseProviders(movieShow.getStreamingProviders()),
                movieShow.getWatchCountry(),
                UserOwnedResponse.from(movieShow.getUserOwned()));
    }

    private static List<StreamingProvider> toDomainProviders(List<StreamingProviderRequest> requests) {
        if (requests == null) {
            return null;
        }
        return requests.stream()
                .map(r -> StreamingProvider.builder()
                        .providerId(r.providerId())
                        .providerName(r.providerName())
                        .logoUrl(r.logoUrl())
                        .type(parseType(r.type()))
                        .build())
                .toList();
    }

    private static List<StreamingProviderResponse> toResponseProviders(List<StreamingProvider> providers) {
        if (providers == null) {
            return null;
        }
        return providers.stream().map(StreamingProviderResponse::from).toList();
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
