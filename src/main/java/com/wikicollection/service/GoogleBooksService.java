package com.wikicollection.service;

import java.util.List;

import com.wikicollection.dto.GoogleBookResult;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.server.ResponseStatusException;

@Service
public class GoogleBooksService {

    private static final String VOLUMES_PATH = "/v1/volumes";
    private static final int MAX_RESULTS = 20;

    private final RestClient restClient;
    private final String apiKey;

    public GoogleBooksService(
            RestClient googleBooksRestClient,
            @Value("${google.books.api-key:}") String apiKey) {
        this.restClient = googleBooksRestClient;
        this.apiKey = apiKey;
    }

    public List<GoogleBookResult> search(String query) {
        if (query == null || query.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El parámetro de búsqueda 'q' es obligatorio");
        }
        try {
            GoogleBooksResponse response = restClient.get()
                    .uri(uriBuilder -> {
                        uriBuilder.path(VOLUMES_PATH)
                                .queryParam("q", query)
                                .queryParam("maxResults", MAX_RESULTS);
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
        } catch (RestClientResponseException e) {
            throw new ResponseStatusException(e.getStatusCode(),
                    "Error al consultar Google Books API", e);
        } catch (ResourceAccessException e) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "Google Books API no disponible", e);
        }
    }

    private GoogleBookResult toResult(GoogleBookItem item) {
        VolumeInfo info = item.volumeInfo();
        if (info == null) {
            return new GoogleBookResult(item.id(), null, null, null, null, null, null, null, null, null, null);
        }
        String isbn = extractIsbn(info.industryIdentifiers());
        return new GoogleBookResult(
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