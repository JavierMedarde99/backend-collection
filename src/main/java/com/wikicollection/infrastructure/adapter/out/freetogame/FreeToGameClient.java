package com.wikicollection.infrastructure.adapter.out.freetogame;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

import com.wikicollection.domain.model.GamePlatform;
import com.wikicollection.domain.model.GameSearchResult;
import com.wikicollection.domain.port.out.ExternalGameCatalogClient;

import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

@Slf4j
@Component("freeToGameClient")
public class FreeToGameClient implements ExternalGameCatalogClient {

    private static final String GAMES_PATH = "/games";

    private final RestClient freeToGameClient;

    public FreeToGameClient(@Qualifier("freeToGameRestClient") RestClient freeToGameClient) {
        this.freeToGameClient = freeToGameClient;
    }

    @Override
    public List<GameSearchResult> search(String query) {
        try {
            FreeToGameResponse[] response = freeToGameClient.get()
                    .uri(uriBuilder -> uriBuilder.path(GAMES_PATH)
                            .queryParam("title", query)
                            .build())
                    .retrieve()
                    .body(FreeToGameResponse[].class);

            return toResults(response);
        } catch (RestClientResponseException e) {
            log.warn("FreeToGame devolvió error {}: {}", e.getStatusCode(), e.getResponseBodyAsString());
            return List.of();
        } catch (ResourceAccessException e) {
            log.warn("FreeToGame no disponible: {}", e.getMessage());
            return List.of();
        }
    }

    @Override
    public List<GameSearchResult> getAllGames() {
        try {
            FreeToGameResponse[] response = freeToGameClient.get()
                    .uri(uriBuilder -> uriBuilder.path(GAMES_PATH).build())
                    .retrieve()
                    .body(FreeToGameResponse[].class);

            return toResults(response);
        } catch (RestClientResponseException e) {
            log.warn("FreeToGame devolvió error {}: {}", e.getStatusCode(), e.getResponseBodyAsString());
            return List.of();
        } catch (ResourceAccessException e) {
            log.warn("FreeToGame no disponible: {}", e.getMessage());
            return List.of();
        }
    }

    private List<GameSearchResult> toResults(FreeToGameResponse[] response) {
        if (response == null) {
            return List.of();
        }
        return Arrays.stream(response)
                .map(this::toResult)
                .toList();
    }

    private GameSearchResult toResult(FreeToGameResponse game) {
        return new GameSearchResult(
                game.id() != null ? game.id().toString() : null,
                game.title(),
                game.shortDescription(),
                game.genre(),
                mapPlatform(game.platform()),
                game.publisher(),
                game.developer(),
                parseDate(game.releaseDate()),
                game.thumbnail(),
                "FreeToGame");
    }

    private GamePlatform mapPlatform(String platform) {
        if (platform == null) {
            return null;
        }
        boolean hasPc = platform.toUpperCase().contains("PC");
        boolean hasWeb = platform.toUpperCase().contains("WEB");
        if (hasPc && hasWeb) {
            return GamePlatform.BOTH;
        }
        if (hasWeb) {
            return GamePlatform.WEB_BROWSER;
        }
        if (hasPc) {
            return GamePlatform.PC;
        }
        return null;
    }

    private LocalDate parseDate(String releaseDate) {
        if (releaseDate == null || releaseDate.isBlank()) {
            return null;
        }
        try {
            return LocalDate.parse(releaseDate);
        } catch (RuntimeException e) {
            return null;
        }
    }

    record FreeToGameResponse(Long id, String title, String thumbnail,
                              @JsonProperty("short_description") String shortDescription,
                              String genre, String platform, String publisher, String developer,
                              @JsonProperty("release_date") String releaseDate) {
    }
}