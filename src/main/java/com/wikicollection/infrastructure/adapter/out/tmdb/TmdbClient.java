package com.wikicollection.infrastructure.adapter.out.tmdb;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.wikicollection.domain.model.MovieMediaType;
import com.wikicollection.domain.model.MovieSearchResult;
import com.wikicollection.domain.model.ProviderAccessType;
import com.wikicollection.domain.model.TmdbWatchProvider;
import com.wikicollection.domain.port.out.ExternalMovieCatalogClient;
import com.wikicollection.domain.port.out.MovieDetailsClient;
import com.wikicollection.domain.port.out.WatchProvidersClient;

import lombok.extern.slf4j.Slf4j;

import com.wikicollection.infrastructure.config.CacheConfig;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

@Slf4j
@Component("tmdbClient")
public class TmdbClient implements ExternalMovieCatalogClient, WatchProvidersClient, MovieDetailsClient {

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
        return search(query, null);
    }

    @Override
    @Cacheable(cacheNames = CacheConfig.MOVIE_SEARCH, key = "#query + '|' + #mediaType")
    public List<MovieSearchResult> search(String query, MovieMediaType mediaType) {
        if (query == null || query.isBlank()) {
            return List.of();
        }
        try {
            List<MovieSearchResult> results = new ArrayList<>();
            if (mediaType == null || mediaType == MovieMediaType.MOVIE) {
                results.addAll(fetch(SEARCH_MOVIE_PATH, query, MovieMediaType.MOVIE));
            }
            if (mediaType == null || mediaType == MovieMediaType.TV) {
                results.addAll(fetch(SEARCH_TV_PATH, query, MovieMediaType.TV));
            }
            return results;
        } catch (RestClientResponseException e) {
            log.warn("TMDB devolvió error {}: {}", e.getStatusCode(), e.getMessage());
            return List.of();
        } catch (ResourceAccessException e) {
            log.warn("TMDB no disponible: {}", e.getMessage());
            return List.of();
        } catch (RestClientException e) {
            log.warn("TMDB falló: {}", e.getMessage());
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

    @Override
    public Map<ProviderAccessType, List<TmdbWatchProvider>> getWatchProviders(
            Long tmdbId, MovieMediaType mediaType, String country) {
        if (tmdbId == null || country == null || country.isBlank()) {
            return emptyProviders();
        }
        String path = mediaType == MovieMediaType.TV
                ? "/tv/{id}/watch/providers"
                : "/movie/{id}/watch/providers";
        try {
            WatchProvidersResponse response = executeWithRetry(() -> tmdbRestClient.get()
                    .uri(uriBuilder -> {
                        uriBuilder.path(path);
                        if (apiKey != null && !apiKey.isBlank()) {
                            uriBuilder.queryParam("api_key", apiKey);
                        }
                        return uriBuilder.build(tmdbId);
                    })
                    .retrieve()
                    .body(WatchProvidersResponse.class));
            if (response == null || response.results() == null) {
                return emptyProviders();
            }
            WatchProvidersByCountry byCountry = response.results().get(country);
            if (byCountry == null) {
                return emptyProviders();
            }
            Map<ProviderAccessType, List<TmdbWatchProvider>> result = new EnumMap<>(ProviderAccessType.class);
            result.put(ProviderAccessType.FLATRATE, toProviders(byCountry.flatrate()));
            result.put(ProviderAccessType.BUY, toProviders(byCountry.buy()));
            result.put(ProviderAccessType.RENT, toProviders(byCountry.rent()));
            return result;
        } catch (RestClientException e) {
            log.warn("TMDB watch/providers falló para {}: {}", tmdbId, e.getMessage());
            return emptyProviders();
        }
    }

    private static Map<ProviderAccessType, List<TmdbWatchProvider>> emptyProviders() {
        Map<ProviderAccessType, List<TmdbWatchProvider>> result = new EnumMap<>(ProviderAccessType.class);
        result.put(ProviderAccessType.FLATRATE, List.of());
        result.put(ProviderAccessType.BUY, List.of());
        result.put(ProviderAccessType.RENT, List.of());
        return result;
    }

    private static List<TmdbWatchProvider> toProviders(List<WatchProvider> providers) {
        if (providers == null) {
            return List.of();
        }
        return providers.stream()
                .map(p -> new TmdbWatchProvider(p.providerId(), p.providerName(), p.logoPath()))
                .toList();
    }

    @Override
    @Cacheable(cacheNames = CacheConfig.MOVIE_GENRES, key = "'genres:' + #tmdbId + '|' + #mediaType")
    public List<String> getGenres(Long tmdbId, MovieMediaType mediaType) {
        if (tmdbId == null) {
            return List.of();
        }
        String path = mediaType == MovieMediaType.TV ? "/tv/{id}" : "/movie/{id}";
        try {
            TmdbDetails details = executeWithRetry(() -> tmdbRestClient.get()
                    .uri(uriBuilder -> {
                        uriBuilder.path(path).queryParam("language", "es-ES");
                        if (apiKey != null && !apiKey.isBlank()) {
                            uriBuilder.queryParam("api_key", apiKey);
                        }
                        return uriBuilder.build(tmdbId);
                    })
                    .retrieve()
                    .body(TmdbDetails.class));
            if (details == null || details.genres() == null) {
                return List.of();
            }
            return details.genres().stream()
                    .map(TmdbGenre::name)
                    .filter(name -> name != null && !name.isBlank())
                    .toList();
        } catch (RestClientException e) {
            log.warn("TMDB detalles falló para {}: {}", tmdbId, e.getMessage());
            return List.of();
        }
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
    record TmdbDetails(List<TmdbGenre> genres) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record TmdbGenre(Long id, String name) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record WatchProvidersResponse(
            Long id,
            Map<String, WatchProvidersByCountry> results) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record WatchProvidersByCountry(
            List<WatchProvider> flatrate,
            List<WatchProvider> buy,
            List<WatchProvider> rent) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record WatchProvider(
            @JsonProperty("provider_id") Integer providerId,
            @JsonProperty("provider_name") String providerName,
            @JsonProperty("logo_path") String logoPath) {
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
