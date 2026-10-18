package com.wikicollection.infrastructure.adapter.out.bgg.xml;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.wikicollection.application.exception.ExternalCatalogException;
import com.wikicollection.domain.model.BoardGameExpansionInfo;
import com.wikicollection.domain.model.BoardGameSearchResult;
import com.wikicollection.domain.port.out.ExternalBoardGameCatalogClient;
import com.wikicollection.infrastructure.adapter.out.bgg.mapper.BoardGameXmlMapper;
import com.wikicollection.infrastructure.adapter.out.bgg.mapper.BoardGameXmlMapper.BggXmlItem;

import lombok.extern.slf4j.Slf4j;
import com.wikicollection.infrastructure.config.CacheConfig;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.util.UriComponentsBuilder;

@Slf4j
@Component("bggXmlClient")
public class BggXmlClient implements ExternalBoardGameCatalogClient {

    private static final String SEARCH_PATH = "/search";
    private static final String THING_PATH = "/thing";
    private static final int MAX_THING_IDS = 20;

    private final RestClient bggXmlRestClient;
    private final String baseUrl;
    private final int retryAttempts;
    private final long retryDelayMs;
    private final BoardGameXmlMapper mapper;

    public BggXmlClient(@Qualifier("bggXmlRestClient") RestClient bggXmlRestClient,
                        @Value("${bgg.api.xml-url:https://boardgamegeek.com/xmlapi2}") String baseUrl,
                        @Value("${bgg.api.retry-attempts:3}") int retryAttempts,
                        @Value("${bgg.api.retry-delay-ms:2000}") long retryDelayMs,
                        BoardGameXmlMapper mapper) {
        this.bggXmlRestClient = bggXmlRestClient;
        this.baseUrl = baseUrl;
        this.retryAttempts = retryAttempts;
        this.retryDelayMs = retryDelayMs;
        this.mapper = mapper;
    }

    @Override
    @Cacheable(cacheNames = CacheConfig.BOARDGAME_SEARCH, key = "#query")
    public List<BoardGameSearchResult> search(String query) {
        try {
            String searchUri = UriComponentsBuilder.fromUriString(baseUrl)
                    .path(SEARCH_PATH)
                    .queryParam("query", query)
                    .queryParam("type", "boardgame")
                    .build()
                    .toUriString();
            ResponseEntity<String> searchResponse = callWithRetry(query, searchUri);
            List<BoardGameSearchResult> results = mapper.map(searchResponse.getBody());

            if (results.isEmpty()) {
                return results;
            }

            List<String> ids = results.stream()
                    .map(BoardGameSearchResult::bggId)
                    .toList();
            if (ids.size() > MAX_THING_IDS) {
                log.info("Búsqueda '{}' devolvió {} resultados; /thing admite máx. {} ids por llamada, se pedirán en {} lotes",
                        query, ids.size(), MAX_THING_IDS, (ids.size() + MAX_THING_IDS - 1) / MAX_THING_IDS);
            }
            Map<String, BggXmlItem> details = fetchAllDetails(ids);

            return results.stream()
                    .map(r -> mapper.enrich(r, details.get(r.bggId())))
                    .toList();
        } catch (RestClientResponseException e) {
            log.warn("BGG XML devolvió error {}: {}", e.getStatusCode(), e.getResponseBodyAsString());
            return List.of();
        } catch (ResourceAccessException e) {
            log.warn("BGG XML no disponible: {}", e.getMessage());
            return List.of();
        }
    }

    /**
     * Expansiones que cuelgan de un juego base: primero la ficha del base para ver
     * sus links, y después las fichas de los hijos (BGG admite 20 ids por llamada).
     * El resultado no depende del usuario, así que se cachea por el bggId del base.
     */
    @Override
    @Cacheable(cacheNames = CacheConfig.BOARDGAME_EXPANSIONS, key = "#baseBggId")
    public List<BoardGameExpansionInfo> expansionsOf(String baseBggId) {
        if (baseBggId == null || baseBggId.isBlank()) {
            return List.of();
        }
        BggXmlItem baseItem = fetchThingsStrict(List.of(baseBggId)).get(baseBggId);
        if (baseItem == null) {
            return List.of();
        }
        List<String> childIds = mapper.expansionIdsOf(baseItem);
        if (childIds.isEmpty()) {
            return List.of();
        }
        Map<String, BggXmlItem> children = fetchAllThingsStrict(childIds);
        List<BoardGameExpansionInfo> expansions = new ArrayList<>();
        for (String childId : childIds) {
            BggXmlItem item = children.get(childId);
            if (item != null) {
                expansions.add(mapper.toExpansionInfo(item));
            }
        }
        return List.copyOf(expansions);
    }

    @Override
    public List<BoardGameExpansionInfo> findByBggIds(List<String> bggIds) {
        if (bggIds == null || bggIds.isEmpty()) {
            return List.of();
        }
        List<String> distinctIds = bggIds.stream()
                .filter(id -> id != null && !id.isBlank())
                .distinct()
                .toList();
        if (distinctIds.isEmpty()) {
            return List.of();
        }
        Map<String, BggXmlItem> things = fetchAllThingsStrict(distinctIds);
        List<BoardGameExpansionInfo> infos = new ArrayList<>();
        for (String id : distinctIds) {
            BggXmlItem item = things.get(id);
            if (item != null) {
                infos.add(mapper.toExpansionInfo(item));
            }
        }
        return List.copyOf(infos);
    }

    private Map<String, BggXmlItem> fetchAllThingsStrict(List<String> ids) {
        Map<String, BggXmlItem> things = new HashMap<>();
        for (int from = 0; from < ids.size(); from += MAX_THING_IDS) {
            things.putAll(fetchThingsStrict(ids.subList(from, Math.min(from + MAX_THING_IDS, ids.size()))));
        }
        return things;
    }

    /**
     * Como {@link #fetchDetails}, pero sin tragarse los errores: quien necesita la
     * ficha para decidir algo (¿es una expansión? ¿de qué base?) no puede confundir
     * un BGG caído con un catálogo vacío.
     */
    private Map<String, BggXmlItem> fetchThingsStrict(List<String> ids) {
        String thingUri = UriComponentsBuilder.fromUriString(baseUrl)
                .path(THING_PATH)
                .queryParam("id", String.join(",", ids))
                .build()
                .toUriString();
        ResponseEntity<String> response;
        try {
            response = callWithRetry("expansiones", thingUri);
        } catch (RestClientResponseException e) {
            if (e.getStatusCode() == HttpStatus.NOT_FOUND) {
                // BGG no conoce estos ids: dato inexistente, no un fallo del servicio.
                log.info("BGG no conoce los ids {}", ids);
                return Map.of();
            }
            log.warn("BGG XML devolvió error {}: {}", e.getStatusCode(), e.getResponseBodyAsString());
            throw new ExternalCatalogException("BGG devolvió error " + e.getStatusCode().value(), e);
        } catch (ResourceAccessException e) {
            log.warn("BGG XML no disponible: {}", e.getMessage());
            throw new ExternalCatalogException("BGG no responde: " + e.getMessage(), e);
        }
        if (response.getStatusCode() == HttpStatus.ACCEPTED) {
            throw new ExternalCatalogException("BGG siguió devolviendo 202 tras los reintentos");
        }
        try {
            return mapper.mapThing(response.getBody());
        } catch (IllegalStateException e) {
            throw new ExternalCatalogException("BGG devolvió una respuesta que no se pudo leer", e);
        }
    }

    private Map<String, BggXmlItem> fetchAllDetails(List<String> ids) {
        Map<String, BggXmlItem> details = new HashMap<>();
        for (int from = 0; from < ids.size(); from += MAX_THING_IDS) {
            details.putAll(fetchDetails(ids.subList(from, Math.min(from + MAX_THING_IDS, ids.size()))));
        }
        return details;
    }

    private Map<String, BggXmlItem> fetchDetails(List<String> ids) {
        try {
            String thingUri = UriComponentsBuilder.fromUriString(baseUrl)
                    .path(THING_PATH)
                    .queryParam("id", String.join(",", ids))
                    .queryParam("stats", "1")
                    .build()
                    .toUriString();
            ResponseEntity<String> response = callWithRetry("details", thingUri);
            return mapper.mapThing(response.getBody());
        } catch (RestClientResponseException e) {
            log.warn("BGG thing devolvió error {}: {}", e.getStatusCode(), e.getResponseBodyAsString());
            return Map.of();
        } catch (ResourceAccessException e) {
            log.warn("BGG thing no disponible: {}", e.getMessage());
            return Map.of();
        }
    }

    private ResponseEntity<String> callWithRetry(String label, String uri) {
        String cookie = null;
        for (int attempt = 1; attempt <= retryAttempts + 1; attempt++) {
            final String currentCookie = cookie;
            ResponseEntity<String> response = bggXmlRestClient.get()
                    .uri(uri)
                    .headers(httpHeaders -> {
                        httpHeaders.setAccept(List.of(MediaType.APPLICATION_XML));
                        if (currentCookie != null && !currentCookie.isBlank()) {
                            httpHeaders.set(HttpHeaders.COOKIE, currentCookie);
                        }
                    })
                    .retrieve()
                    .toEntity(String.class);
            if (response.getStatusCode() != HttpStatus.ACCEPTED) {
                return response;
            }
            String setCookie = firstSetCookie(response);
            if (setCookie != null) {
                cookie = setCookie;
            }
            if (attempt <= retryAttempts) {
                log.info("BGG XML devolvió 202 en '{}', reintentando ({}/{})", label, attempt, retryAttempts);
                sleep(retryDelayMs);
            }
        }
        log.warn("BGG XML siguió devolviendo 202 tras {} reintentos en '{}'", retryAttempts, label);
        return ResponseEntity.status(HttpStatus.ACCEPTED).build();
    }

    private String firstSetCookie(ResponseEntity<String> response) {
        List<String> cookies = response.getHeaders().get(HttpHeaders.SET_COOKIE);
        if (cookies == null || cookies.isEmpty()) {
            return null;
        }
        String full = cookies.get(0);
        int semi = full.indexOf(';');
        return semi >= 0 ? full.substring(0, semi) : full;
    }

    private void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
        }
    }
}
