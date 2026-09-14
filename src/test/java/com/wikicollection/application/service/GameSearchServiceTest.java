package com.wikicollection.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.List;

import com.wikicollection.domain.model.GamePlatform;
import com.wikicollection.domain.model.GameSearchResult;
import com.wikicollection.domain.port.out.ExternalGameCatalogClient;

import org.junit.jupiter.api.BeforeEach;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

class GameSearchServiceTest {

    @Mock
    private ExternalGameCatalogClient rawgClient;

    @Mock
    private ExternalGameCatalogClient freeToGameClient;

    private GameSearchService gameSearchService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        gameSearchService = new GameSearchService(rawgClient, freeToGameClient);
    }

    private GameSearchResult sampleResult(String title) {
        return new GameSearchResult(
                "1", title, "Descripción", "RPG", GamePlatform.PC,
                "CD Projekt", "CD Projekt Red", LocalDate.of(2015, 5, 19),
                "http://img", "RAWG");
    }

    @Test
    void search_returnsRawgResults_whenPresent() {
        GameSearchResult rawg = sampleResult("The Witcher 3");
        when(rawgClient.search("witcher")).thenReturn(List.of(rawg));

        Page<GameSearchResult> results = gameSearchService.search("witcher", PageRequest.of(0, 10));

        assertThat(results.getContent()).containsExactly(rawg);
        assertThat(results.getTotalElements()).isEqualTo(1);
        verify(rawgClient).search("witcher");
        verify(freeToGameClient, never()).search("witcher");
    }

    @Test
    void search_fallsBackToFreeToGame_whenRawgEmpty() {
        GameSearchResult free = new GameSearchResult(
                "1", "The Witcher 3", "Descripción", "RPG", GamePlatform.PC,
                "CD Projekt", "CD Projekt Red", LocalDate.of(2015, 5, 19),
                "http://img", "FreeToGame");
        when(rawgClient.search("witcher")).thenReturn(List.of());
        when(freeToGameClient.search("witcher")).thenReturn(List.of(free));

        Page<GameSearchResult> results = gameSearchService.search("witcher", PageRequest.of(0, 10));

        assertThat(results.getContent()).containsExactly(free);
        verify(rawgClient).search("witcher");
        verify(freeToGameClient).search("witcher");
    }

    @Test
    void search_returnsEmpty_whenBothEmpty() {
        when(rawgClient.search("witcher")).thenReturn(List.of());
        when(freeToGameClient.search("witcher")).thenReturn(List.of());

        Page<GameSearchResult> results = gameSearchService.search("witcher", PageRequest.of(0, 10));

        assertThat(results).isEmpty();
    }

    @Test
    void search_paginatesResults() {
        List<GameSearchResult> rawg = java.util.stream.IntStream.range(0, 8)
                .mapToObj(i -> sampleResult("Juego " + i))
                .toList();
        when(rawgClient.search("witcher")).thenReturn(rawg);

        Page<GameSearchResult> first = gameSearchService.search("witcher", PageRequest.of(0, 5));
        Page<GameSearchResult> second = gameSearchService.search("witcher", PageRequest.of(1, 5));

        assertThat(first.getContent()).isEqualTo(rawg.subList(0, 5));
        assertThat(first.getTotalElements()).isEqualTo(8);
        assertThat(first.getTotalPages()).isEqualTo(2);
        assertThat(second.getContent()).isEqualTo(rawg.subList(5, 8));
    }

    @Test
    void search_rejectsBlankQuery() {
        assertThatThrownBy(() -> gameSearchService.search("   ", PageRequest.of(0, 10)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void search_rejectsNullQuery() {
        assertThatThrownBy(() -> gameSearchService.search(null, PageRequest.of(0, 10)))
                .isInstanceOf(IllegalArgumentException.class);
    }
}