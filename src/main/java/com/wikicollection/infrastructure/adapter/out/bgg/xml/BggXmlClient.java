package com.wikicollection.infrastructure.adapter.out.bgg.xml;

import java.util.List;
import java.util.Map;

import com.wikicollection.domain.model.BoardGameSearchResult;
import com.wikicollection.domain.port.out.ExternalBoardGameCatalogClient;
import com.wikicollection.infrastructure.adapter.out.bgg.mapper.BoardGameXmlMapper;
import com.wikicollection.infrastructure.adapter.out.bgg.mapper.BoardGameXmlMapper.BggXmlItem;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

@Slf4j
@Component("bggXmlClient")
public class BggXmlClient implements ExternalBoardGameCatalogClient {

    private static final String SEARCH_PATH = "/search";
    private static final String THING_PATH = "/thing";
    private static final int MAX_THING_IDS = 20;

    private final RestTemplate bggXmlRestTemplate;
    private final String baseUrl;
    private final int retryAttempts;
    private final long retryDelayMs;
    private final BoardGameXmlMapper mapper;

    public BggXmlClient(@Qualifier("bggXmlRestTemplate") RestTemplate bggXmlRestTemplate,
                        @Value("${bgg.api.xml-url:https://boardgamegeek.com/xmlapi2}") String baseUrl,
                        @Value("${bgg.api.retry-attempts:3}") int retryAttempts,
                        @Value("${bgg.api.retry-delay-ms:2000}") long retryDelayMs,
                        BoardGameXmlMapper mapper) {
        this.bggXmlRestTemplate = bggXmlRestTemplate;
        this.baseUrl = baseUrl;
        this.retryAttempts = retryAttempts;
        this.retryDelayMs = retryDelayMs;
        this.mapper = mapper;
    }

    @Override
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
                    .limit(MAX_THING_IDS)
                    .toList();
            if (results.size() > MAX_THING_IDS) {
                log.warn("Búsqueda '{}' devolvió {} resultados; BGG permite máx. {} ids en /thing, se enriquecerán los primeros {}",
                        query, results.size(), MAX_THING_IDS, MAX_THING_IDS);
            }
            Map<String, BggXmlItem> details = fetchDetails(ids);

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
            HttpEntity<Void> entity = new HttpEntity<>(headers(cookie));
            ResponseEntity<String> response = bggXmlRestTemplate.exchange(uri, HttpMethod.GET, entity, String.class);
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

    private HttpHeaders headers(String cookie) {
        HttpHeaders headers = new HttpHeaders();
        headers.setAccept(List.of(MediaType.APPLICATION_XML));
        if (cookie != null && !cookie.isBlank()) {
            headers.set(HttpHeaders.COOKIE, cookie);
        }
        return headers;
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
