package com.wikicollection.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import com.wikicollection.domain.model.BookSearchResult;
import com.wikicollection.domain.port.out.ExternalBookCatalogClient;
import com.wikicollection.domain.port.out.ExternalBookEnricher;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class BookSearchServiceTest {

    @Mock
    private ExternalBookCatalogClient externalBookCatalogClient;

    @Mock
    private ExternalBookEnricher externalBookEnricher;

    @InjectMocks
    private BookSearchService bookSearchService;

    @Test
    void search_delegatesToClient() {
        BookSearchResult result = new BookSearchResult(
                "abc123", "Cien años de soledad", List.of("Gabriel García Márquez"),
                "9780307474728", "http://thumb", "Sinopsis", 417,
                "Vintage Español", "2011-05-03", 2011, "es", List.of("Literatura"));
        when(externalBookCatalogClient.search("cien", PageRequest.of(0, 10)))
                .thenReturn(new PageImpl<>(List.of(result), PageRequest.of(0, 10), 1));

        Page<BookSearchResult> results = bookSearchService.search("cien", PageRequest.of(0, 10));

        assertThat(results.getContent()).containsExactly(result);
        assertThat(results.getTotalElements()).isEqualTo(1);
        verify(externalBookCatalogClient).search("cien", PageRequest.of(0, 10));
    }

    @Test
    void search_rejectsBlankQuery() {
        assertThatThrownBy(() -> bookSearchService.search("   ", PageRequest.of(0, 10)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void search_rejectsNullQuery() {
        assertThatThrownBy(() -> bookSearchService.search(null, PageRequest.of(0, 10)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void searchByIsbn_delegatesNormalized() {
        BookSearchResult result = new BookSearchResult(
                "abc123", "Dune", List.of("Frank Herbert"),
                "9788498382671", "http://thumb", "Sinopsis", 412,
                "Debolsillo", "2008-01-01", 2008, "es", List.of("Novela"));
        when(externalBookCatalogClient.searchByIsbn("9788498382671")).thenReturn(List.of(result));

        Page<BookSearchResult> results = bookSearchService.searchByIsbn("978-84-9838-267-1", PageRequest.of(0, 10));

        assertThat(results.getContent()).containsExactly(result);
        verify(externalBookCatalogClient).searchByIsbn("9788498382671");
    }

    @Test
    void searchByIsbn_enrichesResults_whenGoogleLacksCoverOrDescription() {
        BookSearchResult raw = new BookSearchResult(
                "LCPqzgEACAAJ", "Fahrenheit 451", List.of("Ray Bradbury"),
                "9788466356718", null, null, 192,
                "DEBOLSILLO", "2021-06", 2021, "es", List.of());
        BookSearchResult enriched = new BookSearchResult(
                "LCPqzgEACAAJ", "Fahrenheit 451", List.of("Ray Bradbury"),
                "9788466356718", "http://portada", "Sinopsis", 192,
                "DEBOLSILLO", "2021-06", 2021, "es", List.of());
        when(externalBookCatalogClient.searchByIsbn("9788466356718")).thenReturn(List.of(raw));
        when(externalBookEnricher.enrich(raw)).thenReturn(enriched);

        Page<BookSearchResult> results = bookSearchService.searchByIsbn("9788466356718", PageRequest.of(0, 10));

        assertThat(results.getContent()).containsExactly(enriched);
        verify(externalBookEnricher).enrich(raw);
    }

    @Test
    void searchByIsbn_keepsOriginal_whenEnricherReturnsNull() {
        BookSearchResult raw = new BookSearchResult(
                "LCPqzgEACAAJ", "Fahrenheit 451", List.of("Ray Bradbury"),
                "9788466356718", null, null, 192,
                "DEBOLSILLO", "2021-06", 2021, "es", List.of());
        when(externalBookCatalogClient.searchByIsbn("9788466356718")).thenReturn(List.of(raw));
        when(externalBookEnricher.enrich(raw)).thenReturn(null);

        Page<BookSearchResult> results = bookSearchService.searchByIsbn("9788466356718", PageRequest.of(0, 10));

        assertThat(results.getContent()).containsExactly(raw);
    }

    @Test
    void searchByIsbn_rejectsBlank() {
        assertThatThrownBy(() -> bookSearchService.searchByIsbn("   ", PageRequest.of(0, 10)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void searchByIsbn_rejectsNull() {
        assertThatThrownBy(() -> bookSearchService.searchByIsbn(null, PageRequest.of(0, 10)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void searchByIsbn_normalizesSpaces() {
        when(externalBookCatalogClient.searchByIsbn("9788498382671")).thenReturn(List.of());

        Page<BookSearchResult> results = bookSearchService.searchByIsbn("978 84 9838 267 1", PageRequest.of(0, 10));

        assertThat(results.getContent()).isEmpty();
        verify(externalBookCatalogClient).searchByIsbn("9788498382671");
    }

    @Test
    void searchByIsbn_returnsEmpty_whenClientEmpty() {
        when(externalBookCatalogClient.searchByIsbn("9788498382671")).thenReturn(List.of());

        Page<BookSearchResult> results = bookSearchService.searchByIsbn("9788498382671", PageRequest.of(0, 10));

        assertThat(results.getContent()).isEmpty();
        assertThat(results.getTotalElements()).isZero();
    }
}