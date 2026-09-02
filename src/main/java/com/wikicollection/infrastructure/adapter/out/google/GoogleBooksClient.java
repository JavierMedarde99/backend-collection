package com.wikicollection.infrastructure.adapter.out.google;

import java.util.List;

import com.wikicollection.domain.model.BookSearchResult;
import com.wikicollection.domain.port.out.ExternalBookCatalogClient;

import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

@Slf4j
@Component
public class GoogleBooksClient implements ExternalBookCatalogClient {

    private static final String VOLUMES_PATH = "/v1/volumes";
    private static final int MAX_RESULTS = 10;
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
    public List<BookSearchResult> search(String query) {
        RestClientResponseException lastHttp = null;
        ResourceAccessException lastConnection = null;
        for (int attempt = 0; attempt < TOTAL_ATTEMPTS; attempt++) {
            try {
                return doSearch(query);
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

    private List<BookSearchResult> doSearch(String query) {
        GoogleBooksResponse response = restClient.get()
                .uri(uriBuilder -> {
                    uriBuilder.path(VOLUMES_PATH)
                            .queryParam("q", "intitle:" + query)
                            .queryParam("maxResults", MAX_RESULTS)
                            .queryParam("langRestrict", "es");
                    if (apiKey != null && !apiKey.isBlank()) {
                        uriBuilder.queryParam("key", apiKey);
                    }
                    return uriBuilder.build();
                })
                .retrieve()
                .body(GoogleBooksResponse.class);

        if (response == null || response.items() == null) {
            return List.of();
        }
        return response.items().stream()
                .map(this::toResult)
                .toList();
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
            return new BookSearchResult(item.id(), null, null, null, null, null, null, null, null, null, null);
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
                info.language(),
                info.categories());
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

    record GoogleBooksResponse(List<GoogleBookItem> items) {
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