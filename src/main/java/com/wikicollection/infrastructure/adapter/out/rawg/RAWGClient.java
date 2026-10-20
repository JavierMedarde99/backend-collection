package com.wikicollection.infrastructure.adapter.out.rawg;

import java.time.LocalDate;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

import com.wikicollection.application.exception.ExternalCatalogException;
import com.wikicollection.domain.model.GameAdditionsPage;
import com.wikicollection.domain.model.GameSearchResult;
import com.wikicollection.domain.port.out.ExternalGameAdditionsCatalogClient;
import com.wikicollection.domain.port.out.ExternalGameCatalogClient;

import lombok.extern.slf4j.Slf4j;

import com.wikicollection.infrastructure.config.CacheConfig;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

@Slf4j
@Component("rawgClient")
public class RAWGClient implements ExternalGameCatalogClient, ExternalGameAdditionsCatalogClient {

    private static final String GAMES_PATH = "/games";
    private static final int ADDITIONS_PAGE_SIZE = 40;

    private final RestClient rawgRestClient;
    private final String apiKey;

    public RAWGClient(
            @Qualifier("rawgRestClient") RestClient rawgRestClient,
            @Value("${rawg.api-key:}") String apiKey) {
        this.rawgRestClient = rawgRestClient;
        this.apiKey = apiKey;
    }

    @Override
    @Cacheable(cacheNames = CacheConfig.GAME_SEARCH, key = "'rawg:' + #query")
    public List<GameSearchResult> search(String query) {
        try {
            RawgResponse response = rawgRestClient.get()
                    .uri(uriBuilder -> {
                        uriBuilder.path(GAMES_PATH)
                                .queryParam("search", query)
                                .queryParam("exclude_additions", "true");
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
    @Cacheable(cacheNames = CacheConfig.GAME_SEARCH, key = "'rawg-genres:' + #externalId")
    public List<String> getGenres(String externalId) {
        if (externalId == null || externalId.isBlank()) {
            return List.of();
        }
        try {
            RawgGame game = rawgRestClient.get()
                    .uri(uriBuilder -> {
                        uriBuilder.path(GAMES_PATH + "/{id}");
                        if (apiKey != null && !apiKey.isBlank()) {
                            uriBuilder.queryParam("key", apiKey);
                        }
                        return uriBuilder.build(externalId.strip());
                    })
                    .retrieve()
                    .body(RawgGame.class);
            if (game == null || game.genres() == null) {
                return List.of();
            }
            return game.genres().stream()
                    .map(RawgGenre::name)
                    .filter(name -> name != null && !name.isBlank())
                    .toList();
        } catch (RestClientResponseException e) {
            log.warn("RAWG detalles devolvió error {}: {}", e.getStatusCode(), e.getResponseBodyAsString());
            return List.of();
        } catch (ResourceAccessException e) {
            log.warn("RAWG no disponible: {}", e.getMessage());
            return List.of();
        }
    }

    /**
     * Trae la primera página de adiciones del juego base. A diferencia de
     * {@link #search(String)}, un fallo del catálogo NO se traga en silencio: el
     * cliente pidió explícitamente las adiciones, así que un 5xx se propaga como
     * {@link ExternalCatalogException} y el controlador responde 502.
     */
    @Override
    @Cacheable(cacheNames = CacheConfig.GAME_ADDITIONS, key = "'rawg-additions:' + #externalId")
    public GameAdditionsPage getAdditions(String externalId) {
        if (externalId == null || externalId.isBlank()) {
            return GameAdditionsPage.empty();
        }
        try {
            RawgResponse response = rawgRestClient.get()
                    .uri(uriBuilder -> {
                        uriBuilder.path(GAMES_PATH + "/{id}/additions")
                                .queryParam("page_size", ADDITIONS_PAGE_SIZE);
                        if (apiKey != null && !apiKey.isBlank()) {
                            uriBuilder.queryParam("key", apiKey);
                        }
                        return uriBuilder.build(externalId.strip());
                    })
                    .retrieve()
                    .body(RawgResponse.class);

            if (response == null || response.results() == null) {
                return GameAdditionsPage.empty();
            }
            List<GameSearchResult> additions = response.results().stream()
                    .map(this::toResult)
                    .toList();
            int totalAvailable = response.count() != null ? response.count() : additions.size();
            return new GameAdditionsPage(additions, totalAvailable);
        } catch (RestClientResponseException e) {
            log.warn("RAWG adiciones devolvió error {}: {}", e.getStatusCode(), e.getResponseBodyAsString());
            throw new ExternalCatalogException("RAWG no pudo devolver las adiciones del juego " + externalId, e);
        } catch (ResourceAccessException e) {
            log.warn("RAWG no disponible: {}", e.getMessage());
            throw new ExternalCatalogException("RAWG no está disponible", e);
        }
    }

    private GameSearchResult toResult(RawgGame game) {
        String genre = firstGenre(game.genres());
        String publisher = firstPublisher(game.publishers());
        String developer = firstDeveloper(game.developers());
        String platform = firstPlatformName(game.platforms());
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

    /**
     * Devuelve el nombre de la primera plataforma con nombre. Antes esto traducía
     * contra un enum de cinco valores y devolvía null para cualquier otra, así que un
     * juego que solo salía en PlayStation 5 llegaba al cliente sin plataforma.
     */
    private String firstPlatformName(List<RawgPlatformEntry> platforms) {
        if (platforms == null || platforms.isEmpty()) {
            return null;
        }
        return platforms.stream()
                .filter(entry -> entry.platform() != null)
                .map(entry -> entry.platform().name())
                .filter(name -> name != null && !name.isBlank())
                .findFirst()
                .orElse(null);
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

    record RawgResponse(Integer count, List<RawgGame> results) {
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