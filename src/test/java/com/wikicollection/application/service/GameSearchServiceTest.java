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

        List<GameSearchResult> results = gameSearchService.search("witcher");

        assertThat(results).containsExactly(rawg);
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

        List<GameSearchResult> results = gameSearchService.search("witcher");

        assertThat(results).containsExactly(free);
        verify(rawgClient).search("witcher");
        verify(freeToGameClient).search("witcher");
    }

    @Test
    void search_returnsEmpty_whenBothEmpty() {
        when(rawgClient.search("witcher")).thenReturn(List.of());
        when(freeToGameClient.search("witcher")).thenReturn(List.of());

        List<GameSearchResult> results = gameSearchService.search("witcher");

        assertThat(results).isEmpty();
    }

    @Test
    void search_rejectsBlankQuery() {
        assertThatThrownBy(() -> gameSearchService.search("   "))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void search_rejectsNullQuery() {
        assertThatThrownBy(() -> gameSearchService.search(null))
                .isInstanceOf(IllegalArgumentException.class);
    }
}