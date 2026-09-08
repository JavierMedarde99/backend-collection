package com.wikicollection.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import com.wikicollection.domain.model.BoardGameSearchResult;
import com.wikicollection.domain.port.out.ExternalBoardGameCatalogClient;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

class BoardGameSearchServiceTest {

    @Mock
    private ExternalBoardGameCatalogClient bggJsonClient;

    @Mock
    private ExternalBoardGameCatalogClient bggXmlClient;

    private BoardGameSearchService boardGameSearchService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        boardGameSearchService = new BoardGameSearchService(bggJsonClient, bggXmlClient);
    }

    private BoardGameSearchResult sampleResult(String title) {
        return new BoardGameSearchResult(
                "31260", title, "Descripción", 2007, 1, 4, 30, 120,
                "Publisher", List.of("Designer"), List.of("Estrategia"), List.of("Dados"),
                "http://img", "http://thumb", new java.math.BigDecimal("8.4"), "BGG");
    }

    @Test
    void search_returnsJsonResults_whenPresent() {
        BoardGameSearchResult json = sampleResult("Catan");
        when(bggJsonClient.search("catan")).thenReturn(List.of(json));

        List<BoardGameSearchResult> results = boardGameSearchService.search("catan");

        assertThat(results).containsExactly(json);
        verify(bggJsonClient).search("catan");
        verify(bggXmlClient, never()).search("catan");
    }

    @Test
    void search_fallsBackToXml_whenJsonEmpty() {
        BoardGameSearchResult xml = sampleResult("Catan en XML");
        when(bggJsonClient.search("catan")).thenReturn(List.of());
        when(bggXmlClient.search("catan")).thenReturn(List.of(xml));

        List<BoardGameSearchResult> results = boardGameSearchService.search("catan");

        assertThat(results).containsExactly(xml);
        verify(bggJsonClient).search("catan");
        verify(bggXmlClient).search("catan");
    }

    @Test
    void search_returnsEmpty_whenBothEmpty() {
        when(bggJsonClient.search("catan")).thenReturn(List.of());
        when(bggXmlClient.search("catan")).thenReturn(List.of());

        List<BoardGameSearchResult> results = boardGameSearchService.search("catan");

        assertThat(results).isEmpty();
    }

    @Test
    void search_limitsResultsToTen() {
        List<BoardGameSearchResult> json = java.util.stream.IntStream.range(0, 15)
                .mapToObj(i -> sampleResult("Juego " + i))
                .toList();
        when(bggJsonClient.search("catan")).thenReturn(json);

        List<BoardGameSearchResult> results = boardGameSearchService.search("catan");

        assertThat(results).hasSize(10);
        assertThat(results).isEqualTo(json.subList(0, 10));
    }

    @Test
    void search_rejectsBlankQuery() {
        assertThatThrownBy(() -> boardGameSearchService.search("   "))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void search_rejectsNullQuery() {
        assertThatThrownBy(() -> boardGameSearchService.search(null))
                .isInstanceOf(IllegalArgumentException.class);
    }
}