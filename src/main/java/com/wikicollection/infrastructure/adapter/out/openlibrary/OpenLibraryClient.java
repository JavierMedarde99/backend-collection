package com.wikicollection.infrastructure.adapter.out.openlibrary;

import java.util.List;
import java.util.Map;

import com.wikicollection.domain.model.BookSearchResult;
import com.wikicollection.domain.port.out.ExternalBookEnricher;
import com.wikicollection.infrastructure.config.CacheConfig;

import lombok.extern.slf4j.Slf4j;

import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/**
 * Rellena la portada y la descripción desde Open Library cuando el resultado de
 * Google Books no las trae (ediciones sin vista previa solo traen metadatos).
 *
 * <p>Una llamada a {@code /isbn/{isbn}.json} da la portada (id de {@code covers})
 * y el {@code works} del que colgar la descripción; si es necesario, una segunda
 * llamada a {@code /works/{key}.json} devuelve la descripción. El resultado se
 * cachea por ISBN.
 */
@Slf4j
@Component
public class OpenLibraryClient implements ExternalBookEnricher {

    private static final String COVER_URL_PATTERN = "https://covers.openlibrary.org/b/id/%s-M.jpg";

    private final RestClient openLibraryRestClient;

    public OpenLibraryClient(RestClient openLibraryRestClient) {
        this.openLibraryRestClient = openLibraryRestClient;
    }

    @Override
    @Cacheable(cacheNames = CacheConfig.BOOK_ENRICH, key = "#result.isbn() ?: ''")
    public BookSearchResult enrich(BookSearchResult result) {
        if (result == null || isBlank(result.isbn())) {
            return result;
        }
        boolean needCover = isBlank(result.coverImage());
        boolean needDescription = isBlank(result.description());
        if (!needCover && !needDescription) {
            return result;
        }
        try {
            Edition edition = fetchEdition(result.isbn());
            if (edition == null) {
                return result;
            }
            String cover = needCover ? coverUrl(edition) : result.coverImage();
            String description = needDescription ? resolveDescription(edition) : result.description();
            return new BookSearchResult(
                    result.id(), result.title(), result.authors(), result.isbn(),
                    cover, description,
                    result.pageCount(), result.publisher(), result.publishedDate(),
                    result.publicationYear(), result.language(), result.categories());
        } catch (RestClientException e) {
            log.warn("No se pudo enriquecer el ISBN {} desde Open Library: {}", result.isbn(), e.getMessage());
            return result;
        }
    }

    private Edition fetchEdition(String isbn) {
        return openLibraryRestClient.get()
                .uri("/isbn/{isbn}.json", isbn)
                .retrieve()
                .body(Edition.class);
    }

    private String coverUrl(Edition edition) {
        if (edition.covers() == null || edition.covers().isEmpty()) {
            return null;
        }
        return COVER_URL_PATTERN.formatted(edition.covers().get(0));
    }

    private String resolveDescription(Edition edition) {
        if (edition.works() == null || edition.works().isEmpty()) {
            return null;
        }
        Work work = openLibraryRestClient.get()
                .uri(edition.works().get(0).key() + ".json")
                .retrieve()
                .body(Work.class);
        return work == null ? null : textOf(work.description());
    }

    private static String textOf(Object description) {
        if (description instanceof String value) {
            return value.isBlank() ? null : value;
        }
        if (description instanceof Map<?, ?> map && map.get("value") instanceof String value && !value.isBlank()) {
            return value;
        }
        return null;
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    record Edition(List<String> covers, List<WorkRef> works) {
    }

    record WorkRef(String key) {
    }

    record Work(Object description) {
    }
}