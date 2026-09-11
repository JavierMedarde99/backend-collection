package com.wikicollection.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import com.wikicollection.domain.model.MagicCard;
import com.wikicollection.domain.model.MagicCardSearchResult;
import com.wikicollection.domain.port.out.ExternalMagicCardCatalogClient;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

class MagicCardSearchServiceTest {

    @Mock
    private ExternalMagicCardCatalogClient scryfallClient;

    private MagicCardSearchService magicCardSearchService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        magicCardSearchService = new MagicCardSearchService(scryfallClient);
    }

    private MagicCardSearchResult sampleResult(String name) {
        return new MagicCardSearchResult("id-1", name, "{R}", "Instant", "uncommon",
                "msc", "Marvel Super Heroes Commander", "http://img", "0.65",
                List.of("R"), List.of("R"), "Lightning Bolt deals 3 damage to any target.");
    }

    @Test
    void search_returnsResultsFromClient() {
        MagicCardSearchResult result = sampleResult("Lightning Bolt");
        when(scryfallClient.search("lightning")).thenReturn(List.of(result));

        List<MagicCardSearchResult> results = magicCardSearchService.search("lightning");

        assertThat(results).containsExactly(result);
        verify(scryfallClient).search("lightning");
    }

    @Test
    void search_returnsEmpty_whenClientEmpty() {
        when(scryfallClient.search("nada")).thenReturn(List.of());

        List<MagicCardSearchResult> results = magicCardSearchService.search("nada");

        assertThat(results).isEmpty();
    }

    @Test
    void search_limitsResultsToTen() {
        List<MagicCardSearchResult> results = java.util.stream.IntStream.range(0, 15)
                .mapToObj(i -> sampleResult("Carta " + i))
                .toList();
        when(scryfallClient.search("carta")).thenReturn(results);

        List<MagicCardSearchResult> result = magicCardSearchService.search("carta");

        assertThat(result).hasSize(10);
        assertThat(result).isEqualTo(results.subList(0, 10));
    }

    @Test
    void search_rejectsBlankQuery() {
        assertThatThrownBy(() -> magicCardSearchService.search("   "))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void search_rejectsNullQuery() {
        assertThatThrownBy(() -> magicCardSearchService.search(null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void findByName_delegatesToClient() {
        MagicCard expected = MagicCard.builder().name("Lightning Bolt").build();
        when(scryfallClient.findByName("lightning bollt")).thenReturn(expected);

        MagicCard result = magicCardSearchService.findByName("lightning bollt");

        assertThat(result).isSameAs(expected);
        verify(scryfallClient).findByName("lightning bollt");
    }

    @Test
    void findByName_rejectsBlankQuery() {
        assertThatThrownBy(() -> magicCardSearchService.findByName(" "))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
