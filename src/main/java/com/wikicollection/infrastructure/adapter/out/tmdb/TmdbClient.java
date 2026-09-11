package com.wikicollection.infrastructure.adapter.out.tmdb;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.wikicollection.domain.model.MovieMediaType;
import com.wikicollection.domain.model.MovieSearchResult;
import com.wikicollection.domain.port.out.ExternalMovieCatalogClient;

import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

@Slf4j
@Component("tmdbClient")
public class TmdbClient implements ExternalMovieCatalogClient {

    private static final String SEARCH_MOVIE_PATH = "/search/movie";
    private static final String SEARCH_TV_PATH = "/search/tv";
    private static final String IMAGE_BASE_URL = "https://image.tmdb.org/t/p/w500";
    private static final String BACKDROP_BASE_URL = "https://image.tmdb.org/t/p/original";

    private final RestClient tmdbRestClient;
    private final String apiKey;
    private final int retryAttempts;
    private final long retryDelayMs;

    public TmdbClient(
            @Qualifier("tmdbRestClient") RestClient tmdbRestClient,
            @Value("${tmdb.api-key:}") String apiKey,
            @Value("${tmdb.api.retry-attempts:3}") int retryAttempts,
            @Value("${tmdb.api.retry-delay-ms:1000}") long retryDelayMs) {
        this.tmdbRestClient = tmdbRestClient;
        this.apiKey = apiKey;
        this.retryAttempts = retryAttempts;
        this.retryDelayMs = retryDelayMs;
    }

    @Override
    public List<MovieSearchResult> search(String query) {
        if (query == null || query.isBlank()) {
            return List.of();
        }
        try {
            List<MovieSearchResult> results = new ArrayList<>();
            results.addAll(fetch(SEARCH_MOVIE_PATH, query, MovieMediaType.MOVIE));
            results.addAll(fetch(SEARCH_TV_PATH, query, MovieMediaType.TV));
            return results;
        } catch (RestClientResponseException e) {
            log.warn("TMDB devolvió error {}: {}", e.getStatusCode(), e.getMessage());
            return List.of();
        } catch (ResourceAccessException e) {
            log.warn("TMDB no disponible: {}", e.getMessage());
            return List.of();
        }
    }

    private List<MovieSearchResult> fetch(String path, String query, MovieMediaType mediaType) {
        TmdbSearchResponse response = executeWithRetry(() -> tmdbRestClient.get()
                .uri(uriBuilder -> {
                    uriBuilder.path(path)
                            .queryParam("query", query)
                            .queryParam("language", "es-ES")
                            .queryParam("page", 1);
                    if (apiKey != null && !apiKey.isBlank()) {
                        uriBuilder.queryParam("api_key", apiKey);
                    }
                    return uriBuilder.build();
                })
                .retrieve()
                .body(TmdbSearchResponse.class));
        if (response == null || response.results() == null) {
            return List.of();
        }
        return response.results().stream()
                .map(entry -> toResult(entry, mediaType))
                .toList();
    }

    private MovieSearchResult toResult(TmdbEntry entry, MovieMediaType mediaType) {
        String title = mediaType == MovieMediaType.MOVIE ? entry.title() : entry.name();
        String date = mediaType == MovieMediaType.MOVIE ? entry.releaseDate() : entry.firstAirDate();
        return new MovieSearchResult(
                entry.id() != null ? entry.id().toString() : null,
                title,
                entry.overview(),
                parseDate(date),
                imageUrl(entry.posterPath(), IMAGE_BASE_URL),
                imageUrl(entry.backdropPath(), BACKDROP_BASE_URL),
                entry.voteAverage(),
                mediaType,
                "TMDB");
    }

    private String imageUrl(String path, String baseUrl) {
        if (path == null || path.isBlank()) {
            return null;
        }
        return baseUrl + path;
    }

    private LocalDate parseDate(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return LocalDate.parse(value);
        } catch (RuntimeException e) {
            return null;
        }
    }

    private <T> T executeWithRetry(java.util.function.Supplier<T> supplier) {
        int attempts = retryAttempts + 1;
        RestClientResponseException last = null;
        for (int i = 0; i < attempts; i++) {
            try {
                return supplier.get();
            } catch (RestClientResponseException e) {
                last = e;
                if (i < attempts - 1) {
                    log.info("TMDB devolvió {}, reintentando ({}/{})", e.getStatusCode(), i + 1, attempts);
                    sleep();
                }
            }
        }
        throw last;
    }

    private void sleep() {
        try {
            Thread.sleep(retryDelayMs);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record TmdbSearchResponse(
            List<TmdbEntry> results) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record TmdbEntry(
            Long id,
            String title,
            String name,
            String overview,
            @JsonProperty("release_date") String releaseDate,
            @JsonProperty("first_air_date") String firstAirDate,
            @JsonProperty("poster_path") String posterPath,
            @JsonProperty("backdrop_path") String backdropPath,
            @JsonProperty("vote_average") Double voteAverage) {
    }
}
