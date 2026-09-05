package com.wikicollection.infrastructure.adapter.out.rawg;

import java.time.LocalDate;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

import com.wikicollection.domain.model.GamePlatform;
import com.wikicollection.domain.model.GameSearchResult;
import com.wikicollection.domain.port.out.ExternalGameCatalogClient;

import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

@Slf4j
@Component("rawgClient")
public class RAWGClient implements ExternalGameCatalogClient {

    private static final String GAMES_PATH = "/games";
    private static final int PAGE_SIZE = 10;

    private final RestClient rawgRestClient;
    private final String apiKey;

    public RAWGClient(
            @Qualifier("rawgRestClient") RestClient rawgRestClient,
            @Value("${rawg.api-key:}") String apiKey) {
        this.rawgRestClient = rawgRestClient;
        this.apiKey = apiKey;
    }

    @Override
    public List<GameSearchResult> search(String query) {
        try {
            RawgResponse response = rawgRestClient.get()
                    .uri(uriBuilder -> {
                        uriBuilder.path(GAMES_PATH)
                                .queryParam("search", query)
                                .queryParam("page_size", PAGE_SIZE);
                        if (apiKey != null && !apiKey.isBlank()) {
                            uriBuilder.queryParam("key", apiKey);
                        }
                        return uriBuilder.build();
                    })
                    .retrieve()
                    .body(RawgResponse.class);

            if (response == null || response.results() == null) {
                return List.of();
            }
            return response.results().stream()
                    .map(this::toResult)
                    .toList();
        } catch (RestClientResponseException e) {
            log.warn("RAWG devolvió error {}: {}", e.getStatusCode(), e.getResponseBodyAsString());
            return List.of();
        } catch (ResourceAccessException e) {
            log.warn("RAWG no disponible: {}", e.getMessage());
            return List.of();
        }
    }

    @Override
    public List<GameSearchResult> getAllGames() {
        try {
            RawgResponse response = rawgRestClient.get()
                    .uri(uriBuilder -> {
                        uriBuilder.path(GAMES_PATH)
                                .queryParam("page_size", PAGE_SIZE);
                        if (apiKey != null && !apiKey.isBlank()) {
                            uriBuilder.queryParam("key", apiKey);
                        }
                        return uriBuilder.build();
                    })
                    .retrieve()
                    .body(RawgResponse.class);

            if (response == null || response.results() == null) {
                return List.of();
            }
            return response.results().stream()
                    .map(this::toResult)
                    .toList();
        } catch (RestClientResponseException e) {
            log.warn("RAWG devolvió error {}: {}", e.getStatusCode(), e.getResponseBodyAsString());
            return List.of();
        } catch (ResourceAccessException e) {
            log.warn("RAWG no disponible: {}", e.getMessage());
            return List.of();
        }
    }

    private GameSearchResult toResult(RawgGame game) {
        String genre = firstGenre(game.genres());
        String publisher = firstPublisher(game.publishers());
        String developer = firstDeveloper(game.developers());
        GamePlatform platform = mapPlatform(game.platforms());
        return new GameSearchResult(
                game.id() != null ? game.id().toString() : null,
                game.name(),
                null,
                genre,
                platform,
                publisher,
                developer,
                parseDate(game.released()),
                game.backgroundImage(),
                "RAWG");
    }

    private GamePlatform mapPlatform(List<RawgPlatformEntry> platforms) {
        if (platforms == null || platforms.isEmpty()) {
            return null;
        }
        return platforms.stream()
                .filter(entry -> entry.platform() != null && entry.platform().name() != null)
                .map(entry -> parsePlatform(entry.platform().name()))
                .filter(java.util.Objects::nonNull)
                .findFirst()
                .orElse(null);
    }

    private GamePlatform parsePlatform(String name) {
        switch (name) {
            case "PC":
                return GamePlatform.PC;
            case "PlayStation 2":
                return GamePlatform.PS2;
            case "PlayStation 3":
                return GamePlatform.PS3;
            case "Wii U":
                return GamePlatform.WII_U;
            case "Nintendo Switch":
                return GamePlatform.SWITCH;
            default:
                return null;
        }
    }

    private String firstGenre(List<RawgGenre> genres) {
        if (genres == null || genres.isEmpty()) {
            return null;
        }
        return genres.get(0).name();
    }

    private String firstPublisher(List<RawgNamedEntity> publishers) {
        if (publishers == null || publishers.isEmpty()) {
            return null;
        }
        return publishers.get(0).name();
    }

    private String firstDeveloper(List<RawgNamedEntity> developers) {
        if (developers == null || developers.isEmpty()) {
            return null;
        }
        return developers.get(0).name();
    }

    private LocalDate parseDate(String released) {
        if (released == null || released.isBlank()) {
            return null;
        }
        try {
            return LocalDate.parse(released);
        } catch (RuntimeException e) {
            return null;
        }
    }

    record RawgResponse(List<RawgGame> results) {
    }

    record RawgGame(Long id, String name, String released,
                    @JsonProperty("background_image") String backgroundImage,
                    List<RawgGenre> genres, List<RawgPlatformEntry> platforms,
                    List<RawgNamedEntity> publishers, List<RawgNamedEntity> developers) {
    }

    record RawgGenre(String name) {
    }

    record RawgPlatformEntry(RawgPlatform platform) {
    }

    record RawgPlatform(String name) {
    }

    record RawgNamedEntity(String name) {
    }
}