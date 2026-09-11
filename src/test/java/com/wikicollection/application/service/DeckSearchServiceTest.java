package com.wikicollection.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import com.wikicollection.domain.model.MagicCardSearchResult;
import com.wikicollection.domain.port.out.ExternalMagicCardCatalogClient;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

class DeckSearchServiceTest {

    @Mock
    private ExternalMagicCardCatalogClient catalogClient;

    private DeckSearchService deckSearchService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        deckSearchService = new DeckSearchService(catalogClient);
    }

    private MagicCardSearchResult sampleResult(String name) {
        return new MagicCardSearchResult("id-1", name, "{R}", "Legendary Creature", "mythic",
                "msc", "Set", "http://img", "0.65", List.of("R"), List.of("R"), "Flying, vigilance.");
    }

    @Test
    void searchCommanders_delegatesToCatalog() {
        MagicCardSearchResult result = sampleResult("Atraxa");
        when(catalogClient.searchCommanders("wubg")).thenReturn(List.of(result));

        List<MagicCardSearchResult> results = deckSearchService.searchCommanders("wubg");

        assertThat(results).containsExactly(result);
        verify(catalogClient).searchCommanders("wubg");
    }

    @Test
    void searchCommanders_limitsResultsToTen() {
        List<MagicCardSearchResult> results = java.util.stream.IntStream.range(0, 15)
                .mapToObj(i -> sampleResult("Comandante " + i))
                .toList();
        when(catalogClient.searchCommanders("")).thenReturn(results);

        List<MagicCardSearchResult> result = deckSearchService.searchCommanders(" ");

        assertThat(result).hasSize(10);
        verify(catalogClient).searchCommanders("");
    }
}
