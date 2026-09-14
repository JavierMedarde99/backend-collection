package com.wikicollection.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import com.wikicollection.domain.model.BoardGameSearchResult;
import com.wikicollection.domain.port.out.ExternalBoardGameCatalogClient;

import org.junit.jupiter.api.BeforeEach;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

class BoardGameSearchServiceTest {

    @Mock
    private ExternalBoardGameCatalogClient bggXmlClient;

    private BoardGameSearchService boardGameSearchService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        boardGameSearchService = new BoardGameSearchService(bggXmlClient);
    }

    private BoardGameSearchResult sampleResult(String title) {
        return new BoardGameSearchResult(
                "31260", title, "Descripción", 2007, 1, 4, 30, 120,
                "Publisher", List.of("Designer"), List.of("Estrategia"), List.of("Dados"),
                "http://img", "http://thumb", new java.math.BigDecimal("8.4"), "BGG");
    }

    @Test
    void search_returnsResults_whenPresent() {
        BoardGameSearchResult result = sampleResult("Catan");
        when(bggXmlClient.search("catan")).thenReturn(List.of(result));

        Page<BoardGameSearchResult> results = boardGameSearchService.search("catan", PageRequest.of(0, 10));

        assertThat(results.getContent()).containsExactly(result);
        assertThat(results.getTotalElements()).isEqualTo(1);
        verify(bggXmlClient).search("catan");
    }

    @Test
    void search_returnsEmpty_whenNoResults() {
        when(bggXmlClient.search("catan")).thenReturn(List.of());

        Page<BoardGameSearchResult> results = boardGameSearchService.search("catan", PageRequest.of(0, 10));

        assertThat(results).isEmpty();
    }

    @Test
    void search_limitsResultsToTen() {
        List<BoardGameSearchResult> many = java.util.stream.IntStream.range(0, 15)
                .mapToObj(i -> sampleResult("Juego " + i))
                .toList();
        when(bggXmlClient.search("catan")).thenReturn(many);

        Page<BoardGameSearchResult> first = boardGameSearchService.search("catan", PageRequest.of(0, 10));
        Page<BoardGameSearchResult> second = boardGameSearchService.search("catan", PageRequest.of(1, 10));

        assertThat(first.getContent()).isEqualTo(many.subList(0, 10));
        assertThat(first.getTotalElements()).isEqualTo(15);
        assertThat(second.getContent()).isEqualTo(many.subList(10, 15));
    }

    @Test
    void search_rejectsBlankQuery() {
        assertThatThrownBy(() -> boardGameSearchService.search("   ", PageRequest.of(0, 10)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void search_rejectsNullQuery() {
        assertThatThrownBy(() -> boardGameSearchService.search(null, PageRequest.of(0, 10)))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
