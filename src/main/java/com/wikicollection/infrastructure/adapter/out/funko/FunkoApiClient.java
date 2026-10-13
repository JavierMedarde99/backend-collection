package com.wikicollection.infrastructure.adapter.out.funko;

import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.wikicollection.domain.model.FunkoCategory;
import com.wikicollection.domain.model.FunkoSearchResult;
import com.wikicollection.domain.port.out.ExternalFunkoCatalogClient;
import com.wikicollection.infrastructure.config.CacheConfig;

import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

/**
 * Cliente del catálogo Funko Pop de HobbyQuantHQ. Todas las llamadas exigen el
 * header {@code x-api-key}; sin él la API responde 422. La búsqueda
 * ({@code /search}) no pagina y trae como mucho 100 resultados, así que se pide
 * el máximo y el corte de página lo hace el servicio. Los sobres de respuesta
 * son {@code results} (búsqueda) y un objeto suelto ({@code /pops/{id}}); el
 * catálogo usa {@code items}, no {@code data}.
 */
@Component
@Slf4j
public class FunkoApiClient implements ExternalFunkoCatalogClient {

    static final long DEFAULT_RETRY_INTERVAL_MILLIS = 1000L;

    private static final int TOTAL_ATTEMPTS = 4;
    /** Tope que impone HobbyQuantHQ: {@code limit} máximo 100 (101 → 422). */
    static final int SEARCH_LIMIT = 100;
    private static final String SEARCH_PATH = "/search";
    private static final String POPS_PATH = "/pops/{id}";
    private static final String CATEGORIES_PATH = "/categories";

    private final RestClient restClient;
    private final String apiKey;
    private final long retryIntervalMillis;

    @Autowired
    public FunkoApiClient(RestClient funkoRestClient,
                          @Value("${hobbyquanthq.api.key:dev-key-change-me}") String apiKey) {
        this(funkoRestClient, apiKey, DEFAULT_RETRY_INTERVAL_MILLIS);
    }

    FunkoApiClient(RestClient funkoRestClient, String apiKey, long retryIntervalMillis) {
        this.restClient = funkoRestClient;
        this.apiKey = apiKey;
        this.retryIntervalMillis = retryIntervalMillis;
    }

    @Override
    @Cacheable(cacheNames = CacheConfig.FUNKO_SEARCH, key = "#query")
    public List<FunkoSearchResult> searchByName(String query) {
        FunkoApiSearchResponse response = withRetry(() -> restClient.get()
                .uri(uriBuilder -> uriBuilder.path(SEARCH_PATH)
                        .queryParam("q", query)
                        .queryParam("limit", SEARCH_LIMIT)
                        .build())
                .header("x-api-key", apiKey)
                .retrieve()
                .body(FunkoApiSearchResponse.class));
        if (response == null || response.results() == null) {
            return List.of();
        }
        return response.results().stream().map(this::toSearchResult).toList();
    }

    @Override
    public Optional<FunkoSearchResult> findById(String externalId) {
        try {
            FunkoApiItem item = withRetry(() -> restClient.get()
                    .uri(POPS_PATH, externalId)
                    .header("x-api-key", apiKey)
                    .retrieve()
                    .body(FunkoApiItem.class));
            if (item == null) {
                return Optional.empty();
            }
            return Optional.of(toSearchResult(item));
        } catch (RestClientResponseException e) {
            if (e.getStatusCode().is4xxClientError()) {
                // HobbyQuantHQ responde 404 cuando el id no existe en el catálogo.
                return Optional.empty();
            }
            throw e;
        }
    }

    @Override
    @Cacheable(cacheNames = CacheConfig.FUNKO_CATEGORIES, key = "'all'")
    public List<FunkoCategory> listCategories() {
        List<FunkoApiCategoryResponse> response = withRetry(() -> restClient.get()
                .uri(CATEGORIES_PATH)
                .header("x-api-key", apiKey)
                .retrieve()
                .body(new org.springframework.core.ParameterizedTypeReference<List<FunkoApiCategoryResponse>>() {
                }));
        if (response == null) {
            return List.of();
        }
        return response.stream()
                .filter(category -> category != null && category.name() != null)
                .map(category -> new FunkoCategory(category.name(), category.count()))
                .toList();
    }

    /**
     * Reintenta 5xx/timeout hasta {@value #TOTAL_ATTEMPTS} intentos (con
     * {@code retryIntervalMillis} entre ellos) y relanza el último error. Un
     * 4xx no es transitorio (p. ej. el 404 de un id desconocido o el 422 de una
     * query de menos de 2 caracteres) y no se reintenta.
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
                log.warn("HobbyQuantHQ devolvió error {}: {}", e.getStatusCode(), e.getResponseBodyAsString());
            } catch (ResourceAccessException e) {
                lastConnection = e;
                log.warn("HobbyQuantHQ no disponible: {}", e.getMessage());
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

    private FunkoSearchResult toSearchResult(FunkoApiItem item) {
        return new FunkoSearchResult(
                item.id() == null ? null : String.valueOf(item.id()),
                item.name(),
                item.series(),
                item.number(),
                item.category(),
                item.exclusive(),
                item.vaulted(),
                item.releaseYear(),
                item.franchise(),
                item.popType(),
                item.rarityScore(),
                item.imageUrl(),
                FunkoSearchResult.EXTERNAL_SOURCE);
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record FunkoApiSearchResponse(List<FunkoApiItem> results, String query, Integer total) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record FunkoApiItem(
            Integer id,
            String name,
            String series,
            String number,
            String category,
            String exclusive,
            Boolean vaulted,
            @JsonProperty("release_year") Integer releaseYear,
            String franchise,
            @JsonProperty("pop_type") String popType,
            @JsonProperty("rarity_score") Integer rarityScore,
            @JsonProperty("image_url") String imageUrl) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record FunkoApiCategoryResponse(String name, Integer count) {
    }
}
