package com.wikicollection.infrastructure.adapter.in.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.Optional;

import com.wikicollection.domain.model.Deck;
import com.wikicollection.domain.model.MagicCard;
import com.wikicollection.domain.model.MagicCardSearchCriteria;
import com.wikicollection.domain.port.out.DeckRepository;
import com.wikicollection.domain.port.out.MagicCardRepository;
import com.wikicollection.infrastructure.adapter.out.scryfall.ScryfallClient;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.client.HttpClientErrorException;

@SpringBootTest(properties = {"spring.data.mongodb.auto-index-creation=false", "app.boardgame-status-migration.enabled=false"})
@AutoConfigureMockMvc
class DeckControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private DeckRepository deckRepository;

    @MockitoBean
    private MagicCardRepository magicCardRepository;

    @MockitoBean
    private ScryfallClient scryfallClient;

    private Deck sampleDeck() {
        return Deck.builder()
                .id("d1")
                .name("Mi Commander")
                .commander("Atraxa, Praetors' Voice")
                .commanderColors(List.of("W", "U", "B", "G"))
                .cards(new java.util.ArrayList<>())
                .build();
    }

    @Test
    void listDecks_returnsPage_whenNoName() throws Exception {
        Deck deck = sampleDeck();
        when(deckRepository.findAll(any(Pageable.class)))
                .thenAnswer(invocation -> new PageImpl<>(
                        List.of(deck), invocation.getArgument(0), 1));

        mockMvc.perform(get("/api/decks"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value("d1"))
                .andExpect(jsonPath("$.content[0].name").value("Mi Commander"))
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.totalPages").value(1));
    }

    @Test
    void listDecks_supportsPaginationAndSort() throws Exception {
        when(deckRepository.findAll(any(Pageable.class))).thenReturn(Page.empty());

        mockMvc.perform(get("/api/decks").param("page", "1").param("size", "5").param("sort", "name,desc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isEmpty());

        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        verify(deckRepository).findAll(captor.capture());
        Pageable pageable = captor.getValue();
        org.assertj.core.api.Assertions.assertThat(pageable.getPageNumber()).isEqualTo(1);
        org.assertj.core.api.Assertions.assertThat(pageable.getPageSize()).isEqualTo(5);
        org.assertj.core.api.Assertions.assertThat(pageable.getSort().getOrderFor("name").getDirection())
                .isEqualTo(org.springframework.data.domain.Sort.Direction.DESC);
    }

    @Test
    void listDecks_returnsEmptyPage_whenNoDecks() throws Exception {
        when(deckRepository.findAll(any(Pageable.class))).thenReturn(Page.empty());

        mockMvc.perform(get("/api/decks"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isEmpty())
                .andExpect(jsonPath("$.totalElements").value(0));
    }

    @Test
    void listDecks_filtersByName() throws Exception {
        Deck deck = sampleDeck();
        when(deckRepository.findByName(eq("Mi Commander"), any(Pageable.class)))
                .thenAnswer(invocation -> new PageImpl<>(
                        List.of(deck), invocation.getArgument(1), 1));

        mockMvc.perform(get("/api/decks").param("name", "Mi Commander"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value("d1"));

        verify(deckRepository).findByName(eq("Mi Commander"), any(Pageable.class));
    }

    @Test
    void getDeck_returnsDeck_whenExists() throws Exception {
        when(deckRepository.findById("d1")).thenReturn(Optional.of(sampleDeck()));

        mockMvc.perform(get("/api/decks/d1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.commander").value("Atraxa, Praetors' Voice"));
    }

    @Test
    void getDeck_returns404_whenMissing() throws Exception {
        when(deckRepository.findById("nope")).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/decks/nope"))
                .andExpect(status().isNotFound());
    }

    @Test
    void createDeck_returns201_withLocation() throws Exception {
        when(deckRepository.save(any(Deck.class))).thenAnswer(invocation -> {
            Deck saved = invocation.getArgument(0);
            saved.setId("d-new");
            return saved;
        });

        mockMvc.perform(post("/api/decks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Nuevo","commander":"Atraxa, Praetors' Voice"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", Matchers.containsString("/api/decks/d-new")))
                .andExpect(jsonPath("$.id").value("d-new"));
    }

    @Test
    void createDeck_returns400_whenBlankName() throws Exception {
        mockMvc.perform(post("/api/decks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":""}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void updateDeck_returnsUpdatedDeck() throws Exception {
        when(deckRepository.findById("d1")).thenReturn(Optional.of(sampleDeck()));
        when(deckRepository.save(any(Deck.class))).thenAnswer(invocation -> invocation.getArgument(0));

        mockMvc.perform(put("/api/decks/d1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Renombrado"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Renombrado"));
    }

    @Test
    void updateDeck_returns404_whenMissing() throws Exception {
        when(deckRepository.findById("nope")).thenReturn(Optional.empty());

        mockMvc.perform(put("/api/decks/nope")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Renombrado"}
                                """))
                .andExpect(status().isNotFound());
    }

    @Test
    void deleteDeck_returns204_whenExists() throws Exception {
        when(deckRepository.findById("d1")).thenReturn(Optional.of(sampleDeck()));

        mockMvc.perform(delete("/api/decks/d1"))
                .andExpect(status().isNoContent());

        verify(deckRepository).deleteById("d1");
    }

    @Test
    void deleteDeck_returns404_whenMissing() throws Exception {
        when(deckRepository.findById("nope")).thenReturn(Optional.empty());

        mockMvc.perform(delete("/api/decks/nope"))
                .andExpect(status().isNotFound());
    }

    @Test
    void addCard_returnsUpdatedDeck() throws Exception {
        MagicCard fetched = MagicCard.builder()
                .scryfallId("sf-1").name("Sol Ring").colorIdentity(List.of())
                .build();
        when(deckRepository.findById("d1")).thenReturn(Optional.of(sampleDeck()));
        when(scryfallClient.findById("sf-1")).thenReturn(fetched);
        when(magicCardRepository.search(any(MagicCardSearchCriteria.class), any(Pageable.class)))
                .thenReturn(Page.empty());
        when(deckRepository.save(any(Deck.class))).thenAnswer(invocation -> invocation.getArgument(0));

        mockMvc.perform(post("/api/decks/d1/cards")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"scryfallId":"sf-1","quantity":1}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cards[0].cardName").value("Sol Ring"))
                .andExpect(jsonPath("$.cards[0].isProxy").value(true));
    }

    @Test
    void addCard_returns404_whenCatalogMissing() throws Exception {
        when(deckRepository.findById("d1")).thenReturn(Optional.of(sampleDeck()));
        when(scryfallClient.findById("missing"))
                .thenThrow(new HttpClientErrorException(HttpStatus.NOT_FOUND));

        mockMvc.perform(post("/api/decks/d1/cards")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"scryfallId":"missing","quantity":1}
                                """))
                .andExpect(status().isNotFound());
    }

    @Test
    void removeCard_returnsUpdatedDeck() throws Exception {
        Deck deck = sampleDeck();
        deck.setCards(new java.util.ArrayList<>(List.of(
                com.wikicollection.domain.model.DeckCard.builder()
                        .cardName("Sol Ring").quantity(1).scryfallId("sf-1").build())));
        when(deckRepository.findById("d1")).thenReturn(Optional.of(deck));
        when(deckRepository.save(any(Deck.class))).thenAnswer(invocation -> invocation.getArgument(0));

        mockMvc.perform(delete("/api/decks/d1/cards/sf-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cards").isEmpty());
    }

    @Test
    void status_returnsDraft_withNullMessage() throws Exception {
        when(deckRepository.findById("d1")).thenReturn(Optional.of(sampleDeck()));

        mockMvc.perform(get("/api/decks/d1/status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("DRAFT"))
                .andExpect(jsonPath("$.message").doesNotExist());
    }

    @Test
    void status_returnsInvalid_withReason() throws Exception {
        Deck deck = sampleDeck();
        deck.setCards(new java.util.ArrayList<>(List.of(
                com.wikicollection.domain.model.DeckCard.builder()
                        .cardName("Black Lotus").quantity(1).scryfallId("sf-x").build())));
        when(deckRepository.findById("d1")).thenReturn(Optional.of(deck));

        mockMvc.perform(get("/api/decks/d1/status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("INVALID"))
                .andExpect(jsonPath("$.message", org.hamcrest.Matchers.containsString("Black Lotus")));
    }
}
