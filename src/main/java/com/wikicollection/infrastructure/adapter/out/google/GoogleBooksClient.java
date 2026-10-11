package com.wikicollection.infrastructure.adapter.out.google;

import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.wikicollection.domain.model.BookSearchResult;
import com.wikicollection.domain.port.out.ExternalBookCatalogClient;

import lombok.extern.slf4j.Slf4j;

import com.wikicollection.infrastructure.config.CacheConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

@Slf4j
@Component
public class GoogleBooksClient implements ExternalBookCatalogClient {

    private static final String VOLUMES_PATH = "/v1/volumes";
    private static final String VIEW_API_URL = "https://books.google.com/books?jscmd=viewapi";
    /** Extrae el id del volumen desde el `info_url` que devuelve la Dynamic Links API. */
    private static final Pattern VOLUME_ID_PATTERN = Pattern.compile("[?&]id=([A-Za-z0-9_-]+)");
    private static final int GOOGLE_MAX_RESULTS = 40;
    private static final int TOTAL_ATTEMPTS = 4;
    private static final long DEFAULT_RETRY_INTERVAL_MILLIS = 1000;

    private final RestClient restClient;
    private final String apiKey;
    private final long retryIntervalMillis;

    @Autowired
    public GoogleBooksClient(
            RestClient googleBooksRestClient,
            @Value("${google.books.api-key:}") String apiKey) {
        this(googleBooksRestClient, apiKey, DEFAULT_RETRY_INTERVAL_MILLIS);
    }

    GoogleBooksClient(
            RestClient googleBooksRestClient,
            String apiKey,
            long retryIntervalMillis) {
        this.restClient = googleBooksRestClient;
        this.apiKey = apiKey;
        this.retryIntervalMillis = retryIntervalMillis;
    }

    @Override
    @Cacheable(cacheNames = CacheConfig.BOOK_SEARCH, key = "#query + ':' + #pageable.offset + ':' + #pageable.pageSize")
    public Page<BookSearchResult> search(String query, Pageable pageable) {
        int startIndex = (int) pageable.getOffset();
        int remaining = pageable.getPageSize();
        List<BookSearchResult> results = new ArrayList<>();
        Integer totalItems = null;
        while (remaining > 0) {
            int maxResults = Math.min(GOOGLE_MAX_RESULTS, remaining);
            GoogleBooksResponse response = executeSearch("intitle:" + query, startIndex, maxResults, "es");
            if (totalItems == null && response != null) {
                totalItems = response.totalItems();
            }
            List<GoogleBookItem> items = response == null || response.items() == null
                    ? List.of()
                    : response.items();
            items.stream().map(this::toResult).forEach(results::add);
            if (items.size() < maxResults) {
                break;
            }
            remaining -= items.size();
            startIndex += items.size();
        }
        long total = totalItems != null ? totalItems : results.size();
        return new PageImpl<>(results, pageable, total);
    }

    @Override
    @Cacheable(cacheNames = CacheConfig.BOOK_SEARCH, key = "'isbn:' + #isbn")
    public List<BookSearchResult> searchByIsbn(String isbn) {
        // Un ISBN ya identifica una edición concreta: no se restringe por idioma.
        // Google aplica langRestrict a los volúmenes "etiquetados" con ese idioma y
        // descarta en silencio ediciones sin etiquetar, devolviendo 0 resultados.
        List<BookSearchResult> results = mapItems(executeSearch("isbn:" + isbn, null, null, null));
        if (!results.isEmpty()) {
            return results;
        }
        // El buscador de Google no siempre indexa el ISBN de las ediciones sin vista
        // previa y responde 0 resultados aunque el volumen exista. La Dynamic Links
        // API de Google (documentada) resuelve ISBN-10/13 al id del volumen, que sí
        // se puede consultar directamente. Es el caso de 9788466356718 -> LCPqzgEACAAJ.
        return searchByResolvedVolumeIds(isbn);
    }

    @Override
    @Cacheable(cacheNames = CacheConfig.BOOK_SEARCH, key = "'volume:' + #volumeId")
    public List<String> getCategories(String volumeId) {
        if (volumeId == null || volumeId.isBlank()) {
            return List.of();
        }
        try {
            GoogleBookItem item = restClient.get()
                    .uri(uriBuilder -> {
                        uriBuilder.path(VOLUMES_PATH + "/{id}");
                        if (apiKey != null && !apiKey.isBlank()) {
                            uriBuilder.queryParam("key", apiKey);
                        }
                        return uriBuilder.build(volumeId.strip());
                    })
                    .retrieve()
                    .body(GoogleBookItem.class);
            if (item == null || item.volumeInfo() == null || item.volumeInfo().categories() == null) {
                return List.of();
            }
            return item.volumeInfo().categories().stream()
                    .filter(c -> c != null && !c.isBlank())
                    .toList();
        } catch (RestClientResponseException e) {
            log.warn("Google Books volumen devolvió error {}: {}", e.getStatusCode(), e.getResponseBodyAsString());
            return List.of();
        } catch (ResourceAccessException e) {
            log.warn("Google Books API no disponible: {}", e.getMessage());
            return List.of();
        }
    }

    private GoogleBooksResponse executeSearch(String q, Integer startIndex, Integer maxResults, String language) {
        return withRetry(() -> doSearch(q, startIndex, maxResults, language));
    }

    private List<BookSearchResult> mapItems(GoogleBooksResponse response) {
        if (response == null || response.items() == null) {
            return List.of();
        }
        return response.items().stream()
                .map(this::toResult)
                .toList();
    }

    private List<BookSearchResult> searchByResolvedVolumeIds(String isbn) {
        List<String> volumeIds;
        try {
            volumeIds = withRetry(() -> resolveVolumeIds(isbn));
        } catch (RestClientException e) {
            log.warn("No se pudo resolver el ISBN {} con la View API de Google Books: {}", isbn, e.getMessage());
            return List.of();
        }
        List<BookSearchResult> results = new ArrayList<>();
        for (String volumeId : volumeIds) {
            try {
                GoogleBookItem item = withRetry(() -> fetchVolume(volumeId));
                if (item != null && item.volumeInfo() != null) {
                    results.add(toResult(item));
                }
            } catch (RestClientException e) {
                log.warn("No se pudo recuperar el volumen {} de Google Books: {}", volumeId, e.getMessage());
            }
        }
        return results;
    }

    /**
     * Resuelve un ISBN al id de su volumen con la Dynamic Links API de Google
     * ({@code jscmd=viewapi}). Acepta ISBN-10 y ISBN-13 y también resuelve ediciones
     * que el buscador {@code q=isbn:} no indexa (devuelve 0 resultados aunque el
     * volumen exista).
     */
    private List<String> resolveVolumeIds(String isbn) {
        String url = VIEW_API_URL + "&bibkeys="
                + URLEncoder.encode("ISBN:" + isbn, StandardCharsets.UTF_8);
        String body = restClient.get()
                .uri(URI.create(url))
                .retrieve()
                .body(String.class);
        if (body == null || body.isBlank()) {
            return List.of();
        }
        Matcher matcher = VOLUME_ID_PATTERN.matcher(body);
        List<String> ids = new ArrayList<>();
        while (matcher.find()) {
            String id = matcher.group(1);
            if (!ids.contains(id)) {
                ids.add(id);
            }
        }
        return ids;
    }

    private GoogleBookItem fetchVolume(String volumeId) {
        return restClient.get()
                .uri(uriBuilder -> {
                    uriBuilder.path(VOLUMES_PATH + "/{id}");
                    if (apiKey != null && !apiKey.isBlank()) {
                        uriBuilder.queryParam("key", apiKey);
                    }
                    return uriBuilder.build(volumeId);
                })
                .retrieve()
                .body(GoogleBookItem.class);
    }

    private <T> T withRetry(Supplier<T> action) {
        RestClientResponseException lastHttp = null;
        ResourceAccessException lastConnection = null;
        for (int attempt = 0; attempt < TOTAL_ATTEMPTS; attempt++) {
            try {
                return action.get();
            } catch (RestClientResponseException e) {
                lastHttp = e;
                log.warn("Google Books API devolvió error {}: {}", e.getStatusCode(), e.getResponseBodyAsString());
            } catch (ResourceAccessException e) {
                lastConnection = e;
                log.warn("Google Books API no disponible: {}", e.getMessage());
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

    private GoogleBooksResponse doSearch(String q, Integer startIndex, Integer maxResults, String language) {
        return restClient.get()
                .uri(uriBuilder -> {
                    uriBuilder.path(VOLUMES_PATH)
                            .queryParam("q", q);
                    if (language != null) {
                        uriBuilder.queryParam("langRestrict", language);
                    }
                    if (startIndex != null) {
                        uriBuilder.queryParam("startIndex", startIndex);
                    }
                    if (maxResults != null) {
                        uriBuilder.queryParam("maxResults", maxResults);
                    }
                    if (apiKey != null && !apiKey.isBlank()) {
                        uriBuilder.queryParam("key", apiKey);
                    }
                    return uriBuilder.build();
                })
                .retrieve()
                .body(GoogleBooksResponse.class);
    }

    private void sleepBeforeRetry() {
        try {
            Thread.sleep(retryIntervalMillis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private BookSearchResult toResult(GoogleBookItem item) {
        VolumeInfo info = item.volumeInfo();
        if (info == null) {
            return new BookSearchResult(item.id(), null, null, null, null, null, null, null, null, null, null, null);
        }
        String isbn = extractIsbn(info.industryIdentifiers());
        return new BookSearchResult(
                item.id(),
                info.title(),
                info.authors(),
                isbn,
                info.imageLinks() != null ? info.imageLinks().thumbnail() : null,
                info.description(),
                info.pageCount(),
                info.publisher(),
                info.publishedDate(),
                extractPublicationYear(info.publishedDate()),
                info.language(),
                info.categories());
    }

    /**
     * Google Books devuelve {@code publishedDate} como texto de precisión variable
     * ("2011", "2011-05", "2011-05-03"). Se extraen los cuatro primeros dígitos
     * para poder ofrecer el año ya normalizado, y se devuelve {@code null} cuando
     * el valor no es un año plausible.
     */
    private static Integer extractPublicationYear(String publishedDate) {
        if (publishedDate == null) {
            return null;
        }
        String trimmed = publishedDate.trim();
        if (trimmed.length() < 4) {
            return null;
        }
        String head = trimmed.substring(0, 4);
        for (int i = 0; i < head.length(); i++) {
            if (!Character.isDigit(head.charAt(i))) {
                return null;
            }
        }
        if (trimmed.length() > 4 && Character.isDigit(trimmed.charAt(4))) {
            return null;
        }
        int year = Integer.parseInt(head);
        return year > 0 ? year : null;
    }

    private String extractIsbn(List<IndustryIdentifier> identifiers) {
        if (identifiers == null || identifiers.isEmpty()) {
            return null;
        }
        return identifiers.stream()
                .filter(id -> id.type() != null && id.type().equalsIgnoreCase("ISBN_13"))
                .map(IndustryIdentifier::identifier)
                .findFirst()
                .orElseGet(() -> identifiers.stream()
                        .filter(id -> id.type() != null && id.type().equalsIgnoreCase("ISBN_10"))
                        .map(IndustryIdentifier::identifier)
                        .findFirst()
                        .orElse(null));
    }

    record GoogleBooksResponse(Integer totalItems, List<GoogleBookItem> items) {
    }

    record GoogleBookItem(String id, VolumeInfo volumeInfo) {
    }

    record VolumeInfo(String title, List<String> authors, String publisher, String publishedDate,
                      String description, Integer pageCount, List<String> categories, String language,
                      ImageLinks imageLinks, List<IndustryIdentifier> industryIdentifiers) {
    }

    record ImageLinks(String thumbnail) {
    }

    record IndustryIdentifier(String type, String identifier) {
    }
}