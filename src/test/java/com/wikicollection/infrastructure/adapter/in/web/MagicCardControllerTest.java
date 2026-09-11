package com.wikicollection.infrastructure.adapter.in.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.Optional;

import com.wikicollection.domain.model.MagicCard;
import com.wikicollection.domain.model.MagicCardSearchCriteria;
import com.wikicollection.domain.model.MagicCardSearchResult;
import com.wikicollection.domain.port.out.MagicCardRepository;
import com.wikicollection.infrastructure.adapter.out.scryfall.ScryfallClient;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.client.HttpClientErrorException;

@SpringBootTest(properties = {"spring.data.mongodb.auto-index-creation=false", "app.boardgame-status-migration.enabled=false"})
@AutoConfigureMockMvc
class MagicCardControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private MagicCardRepository magicCardRepository;

    @MockitoBean
    private ScryfallClient scryfallClient;

    private MagicCard sampleCard() {
        return MagicCard.builder()
                .id("mc1")
                .name("Lightning Bolt")
                .rarity("uncommon")
                .quantity(1)
                .isFoil(false)
                .build();
    }

    @Test
    void listCards_returnsEmptyPage_whenNoCards() throws Exception {
        when(magicCardRepository.search(any(MagicCardSearchCriteria.class), any(Pageable.class)))
                .thenReturn(Page.empty());

        mockMvc.perform(get("/api/magic"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content").isEmpty());
    }

    @Test
    void listCards_filtersByName() throws Exception {
        when(magicCardRepository.search(any(MagicCardSearchCriteria.class), any(Pageable.class)))
                .thenReturn(Page.empty());

        mockMvc.perform(get("/api/magic").param("name", "lightning"))
                .andExpect(status().isOk());

        ArgumentCaptor<MagicCardSearchCriteria> captor = ArgumentCaptor.forClass(MagicCardSearchCriteria.class);
        verify(magicCardRepository).search(captor.capture(), any(Pageable.class));
        org.assertj.core.api.Assertions.assertThat(captor.getValue().name()).isEqualTo("lightning");
    }

    @Test
    void listCards_filtersByAllCriteria() throws Exception {
        when(magicCardRepository.search(any(MagicCardSearchCriteria.class), any(Pageable.class)))
                .thenReturn(Page.empty());

        mockMvc.perform(get("/api/magic")
                        .param("name", "bolt")
                        .param("rarity", "rare")
                        .param("color", "R")
                        .param("type", "Instant"))
                .andExpect(status().isOk());

        ArgumentCaptor<MagicCardSearchCriteria> captor = ArgumentCaptor.forClass(MagicCardSearchCriteria.class);
        verify(magicCardRepository).search(captor.capture(), any(Pageable.class));
        MagicCardSearchCriteria criteria = captor.getValue();
        org.assertj.core.api.Assertions.assertThat(criteria.name()).isEqualTo("bolt");
        org.assertj.core.api.Assertions.assertThat(criteria.rarity()).isEqualTo("rare");
        org.assertj.core.api.Assertions.assertThat(criteria.color()).isEqualTo("R");
        org.assertj.core.api.Assertions.assertThat(criteria.type()).isEqualTo("Instant");
    }

    @Test
    void getCard_returnsCard_whenExists() throws Exception {
        when(magicCardRepository.findById("mc1")).thenReturn(Optional.of(sampleCard()));

        mockMvc.perform(get("/api/magic/mc1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("mc1"))
                .andExpect(jsonPath("$.name").value("Lightning Bolt"))
                .andExpect(jsonPath("$.rarity").value("uncommon"));
    }

    @Test
    void getCard_returns404_whenMissing() throws Exception {
        when(magicCardRepository.findById("nope")).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/magic/nope"))
                .andExpect(status().isNotFound());
    }

    @Test
    void deleteCard_returns204_whenExists() throws Exception {
        when(magicCardRepository.findById("mc1")).thenReturn(Optional.of(sampleCard()));

        mockMvc.perform(delete("/api/magic/mc1"))
                .andExpect(status().isNoContent());

        verify(magicCardRepository).deleteById("mc1");
    }

    @Test
    void deleteCard_returns404_whenMissing() throws Exception {
        when(magicCardRepository.findById("nope")).thenReturn(Optional.empty());

        mockMvc.perform(delete("/api/magic/nope"))
                .andExpect(status().isNotFound());
    }

    @Test
    void addFromScryfall_returns201_withLocationAndColorIdentity() throws Exception {
        MagicCard fetched = MagicCard.builder()
                .scryfallId("sf-1")
                .name("Lightning Bolt")
                .colors(List.of("R"))
                .colorIdentity(List.of("R"))
                .build();
        when(scryfallClient.findById("sf-1")).thenReturn(fetched);
        when(magicCardRepository.save(fetched)).thenAnswer(invocation -> {
            MagicCard saved = invocation.getArgument(0);
            saved.setId("mc1");
            return saved;
        });

        mockMvc.perform(post("/api/magic/scryfall/sf-1"))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", Matchers.containsString("/api/magic/mc1")))
                .andExpect(jsonPath("$.id").value("mc1"))
                .andExpect(jsonPath("$.scryfallId").value("sf-1"))
                .andExpect(jsonPath("$.colorIdentity[0]").value("R"));
    }

    @Test
    void addFromScryfall_returns404_whenCatalogMissing() throws Exception {
        when(scryfallClient.findById("missing")).thenThrow(
                new HttpClientErrorException(HttpStatus.NOT_FOUND));

        mockMvc.perform(post("/api/magic/scryfall/missing"))
                .andExpect(status().isNotFound());
    }

    @Test
    void search_returnsResultsFromScryfall() throws Exception {
        MagicCardSearchResult result = new MagicCardSearchResult(
                "id-1", "Lightning Bolt", "{R}", "Instant", "uncommon",
                "msc", "Marvel Super Heroes Commander", "http://img", "0.65",
                List.of("R"), List.of("R"), "Lightning Bolt deals 3 damage to any target.");
        when(scryfallClient.search("lightning")).thenReturn(List.of(result));

        mockMvc.perform(get("/api/magic/search").param("name", "lightning"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.query").value("lightning"))
                .andExpect(jsonPath("$.results[0].name").value("Lightning Bolt"))
                .andExpect(jsonPath("$.results[0].setName").value("Marvel Super Heroes Commander"))
                .andExpect(jsonPath("$.results[0].colorIdentity[0]").value("R"))
                .andExpect(jsonPath("$.results[0].text").value("Lightning Bolt deals 3 damage to any target."));
    }

    @Test
    void search_returns400_whenBlankQuery() throws Exception {
        mockMvc.perform(get("/api/magic/search").param("name", " "))
                .andExpect(status().isBadRequest());
    }

    @Test
    void commanders_returnsResultsFromScryfall() throws Exception {
        MagicCardSearchResult result = new MagicCardSearchResult(
                "id-9", "Atraxa, Praetors' Voice", "{W}{U}{B}{G}", "Legendary Creature", "mythic",
                "msc", "Set", "http://img", "5.00",
                List.of("W", "U", "B", "G"), List.of("W", "U", "B", "G"), "Flying, vigilance.");
        when(scryfallClient.searchCommanders("wubg")).thenReturn(List.of(result));

        mockMvc.perform(get("/api/magic/commanders").param("colors", "wubg"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.results[0].name").value("Atraxa, Praetors' Voice"))
                .andExpect(jsonPath("$.results[0].colorIdentity[0]").value("W"));
    }

    @Test
    void cors_allowsFrontendOrigin() throws Exception {
        mockMvc.perform(options("/api/magic")
                        .header("Origin", "http://localhost:5173")
                        .header("Access-Control-Request-Method", "GET"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:5173"));
    }
}
