package com.wikicollection.infrastructure.adapter.out.amiibo;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.stream.Stream;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.wikicollection.domain.model.AmiiboReleaseDates;
import com.wikicollection.domain.model.AmiiboSearchResult;
import com.wikicollection.domain.model.AmiiboType;
import com.wikicollection.domain.port.out.ExternalAmiiboCatalogClient;
import com.wikicollection.infrastructure.config.CacheConfig;

import lombok.extern.slf4j.Slf4j;

import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

/**
 * Cliente del catálogo AmiiboAPI. Sin auth y sin paginación: la búsqueda
 * devuelve todos los que coinciden y la paginación es competencia del
 * repositorio local. Los nombres de los parámetros distinguen mayúsculas
 * ({@code name}, {@code id}, {@code showusage}); el cliente los construye
 * exactos porque AmiiboAPI ignora en silencio los desconocidos.
 */
@Component
@Slf4j
public class AmiiboApiClient implements ExternalAmiiboCatalogClient {

    static final long DEFAULT_RETRY_INTERVAL_MILLIS = 1000L;

    private static final int TOTAL_ATTEMPTS = 4;
    private static final String AMIIBO_PATH = "/amiibo/";

    private final RestClient restClient;
    private final long retryIntervalMillis;

    public AmiiboApiClient(RestClient amiiboRestClient) {
        this(amiiboRestClient, DEFAULT_RETRY_INTERVAL_MILLIS);
    }

    AmiiboApiClient(RestClient amiiboRestClient, long retryIntervalMillis) {
        this.restClient = amiiboRestClient;
        this.retryIntervalMillis = retryIntervalMillis;
    }

    @Override
    @Cacheable(cacheNames = CacheConfig.AMIIBO_SEARCH, key = "#query")
    public List<AmiiboSearchResult> searchByName(String query) {
        AmiiboApiListResponse response = withRetry(() -> restClient.get()
                .uri(uriBuilder -> uriBuilder.path(AMIIBO_PATH)
                        .queryParam("name", query)
                        .queryParam("showusage", "true")
                        .build())
                .retrieve()
                .body(AmiiboApiListResponse.class));
        if (response == null || response.amiibo() == null) {
            return List.of();
        }
        return response.amiibo().stream().map(this::toSearchResult).toList();
    }

    @Override
    public Optional<AmiiboSearchResult> findById(String externalId) {
        try {
            AmiiboApiDetailResponse response = withRetry(() -> restClient.get()
                    .uri(uriBuilder -> uriBuilder.path(AMIIBO_PATH)
                            .queryParam("id", externalId)
                            .build())
                    .retrieve()
                    .body(AmiiboApiDetailResponse.class));
            if (response == null || response.amiibo() == null) {
                return Optional.empty();
            }
            return Optional.of(toSearchResult(response.amiibo()));
        } catch (RestClientResponseException e) {
            if (e.getStatusCode().is4xxClientError()) {
                // AmiiboAPI responde 400 cuando el id no existe en el catálogo.
                return Optional.empty();
            }
            throw e;
        }
    }

    /**
     * Reintenta 5xx/timeout hasta {@value #TOTAL_ATTEMPTS} intentos (con
     * {@code retryIntervalMillis} entre ellos) y relanza el último error. Un
     * 4xx no es transitorio (p. ej. el 400 de un id desconocido) y no se
     * reintenta.
     */
    private <T> T withRetry(Supplier<T> action) {
        RestClientResponseException lastHttp = null;
        ResourceAccessException lastConnection = null;
        for (int attempt = 0; attempt < TOTAL_ATTEMPTS; attempt++) {
            try {
                return action.get();
            } catch (RestClientResponseException e) {
                if (e.getStatusCode().is4xxClientError()) {
                    throw e;
                }
                lastHttp = e;
                log.warn("AmiiboAPI devolvió error {}: {}", e.getStatusCode(), e.getResponseBodyAsString());
            } catch (ResourceAccessException e) {
                lastConnection = e;
                log.warn("AmiiboAPI no disponible: {}", e.getMessage());
            }
            if (attempt < TOTAL_ATTEMPTS - 1) {
                sleepBeforeRetry();
            }
        }
        if (lastHttp != null) {
            throw lastHttp;
        }
        throw lastConnection;
    }

    private void sleepBeforeRetry() {
        try {
            Thread.sleep(retryIntervalMillis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private AmiiboSearchResult toSearchResult(AmiiboApiItem item) {
        return new AmiiboSearchResult(
                joinExternalId(item.head(), item.tail()),
                item.name(),
                item.character(),
                item.amiiboSeries(),
                item.gameSeries(),
                toType(item.type()),
                toReleaseDates(item.release()),
                item.image(),
                collectGames(item),
                AmiiboSearchResult.EXTERNAL_SOURCE);
    }

    private static String joinExternalId(String head, String tail) {
        if (head == null && tail == null) {
            return null;
        }
        return (head == null ? "" : head) + (tail == null ? "" : tail);
    }

    private static AmiiboType toType(String type) {
        if (type == null || type.isBlank()) {
            return null;
        }
        try {
            return AmiiboType.valueOf(type.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private static AmiiboReleaseDates toReleaseDates(AmiiboApiRelease release) {
        if (release == null) {
            return null;
        }
        return new AmiiboReleaseDates(
                parseDate(release.au()),
                parseDate(release.eu()),
                parseDate(release.jp()),
                parseDate(release.na()));
    }

    private static LocalDate parseDate(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return LocalDate.parse(value.trim());
        } catch (DateTimeParseException e) {
            return null;
        }
    }

    private static List<String> collectGames(AmiiboApiItem item) {
        return Stream.of(item.games3DS(), item.gamesWiiU(), item.gamesSwitch(), item.gamesSwitch2())
                .filter(list -> list != null)
                .flatMap(List::stream)
                .filter(game -> game != null && game.gameName() != null && !game.gameName().isBlank())
                .map(AmiiboApiGame::gameName)
                .distinct()
                .toList();
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record AmiiboApiListResponse(List<AmiiboApiItem> amiibo) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record AmiiboApiDetailResponse(AmiiboApiItem amiibo) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record AmiiboApiItem(String head, String tail, String name, String character, String amiiboSeries,
                         String gameSeries, String type, String image, AmiiboApiRelease release,
                         List<AmiiboApiGame> games3DS, List<AmiiboApiGame> gamesWiiU,
                         List<AmiiboApiGame> gamesSwitch, List<AmiiboApiGame> gamesSwitch2) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record AmiiboApiRelease(String au, String eu, String jp, String na) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record AmiiboApiGame(String gameName) {
    }
}