package com.wikicollection.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import com.wikicollection.application.exception.DeckNotFoundException;
import com.wikicollection.application.exception.MagicCardNotFoundException;
import com.wikicollection.domain.model.Deck;
import com.wikicollection.domain.model.DeckCard;
import com.wikicollection.domain.model.DeckStatus;
import com.wikicollection.domain.model.DeckStatusReport;
import com.wikicollection.domain.model.MagicCard;
import com.wikicollection.domain.model.MagicCardSearchCriteria;
import com.wikicollection.domain.port.out.DeckRepository;
import com.wikicollection.domain.port.out.ExternalMagicCardCatalogClient;
import com.wikicollection.domain.port.out.MagicCardRepository;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpClientErrorException;

@ExtendWith(MockitoExtension.class)
class DeckServiceTest {

    @Mock
    private DeckRepository deckRepository;

    @Mock
    private ExternalMagicCardCatalogClient catalogClient;

    @Mock
    private MagicCardRepository magicCardRepository;

    private final DeckValidator validator = new DeckValidator();

    private DeckService deckService() {
        return new DeckService(deckRepository, catalogClient, magicCardRepository, validator);
    }

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
    void findById_returnsDeck_whenExists() {
        Deck deck = sampleDeck();
        when(deckRepository.findById("d1")).thenReturn(Optional.of(deck));

        assertThat(deckService().findById("d1")).isSameAs(deck);
    }

    @Test
    void findById_throwsNotFound_whenMissing() {
        when(deckRepository.findById("nope")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> deckService().findById("nope"))
                .isInstanceOf(DeckNotFoundException.class)
                .hasMessageContaining("nope");
    }

    @Test
    void save_setsTimestamps() {
        Deck deck = Deck.builder().name("Nuevo").build();
        when(deckRepository.save(any(Deck.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Deck result = deckService().save(deck);

        assertThat(result.getCreatedAt()).isNotNull();
        assertThat(result.getUpdatedAt()).isNotNull();
    }

    @Test
    void save_rejectsBlankName() {
        assertThatThrownBy(() -> deckService().save(Deck.builder().name(" ").build()))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void update_copiesFields() {
        Deck existing = sampleDeck();
        Deck updates = Deck.builder().name("Renombrado").description("Desc").build();
        when(deckRepository.findById("d1")).thenReturn(Optional.of(existing));
        when(deckRepository.save(any(Deck.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Deck result = deckService().update("d1", updates);

        assertThat(result.getId()).isEqualTo("d1");
        assertThat(result.getName()).isEqualTo("Renombrado");
        assertThat(result.getDescription()).isEqualTo("Desc");
    }

    @Test
    void delete_deletesDeck_whenExists() {
        when(deckRepository.findById("d1")).thenReturn(Optional.of(sampleDeck()));

        deckService().delete("d1");

        verify(deckRepository).deleteById("d1");
    }

    @Test
    void addCard_enrichesFromCatalog_andMarksOwned() {
        Deck deck = sampleDeck();
        MagicCard fetched = MagicCard.builder()
                .scryfallId("sf-1").name("Sol Ring").manaCost("{1}")
                .type("Artifact").colorIdentity(List.of()).imageUrl("http://img")
                .build();
        when(deckRepository.findById("d1")).thenReturn(Optional.of(deck));
        when(catalogClient.findById("sf-1")).thenReturn(fetched);
        when(magicCardRepository.search(any(MagicCardSearchCriteria.class), any(Pageable.class)))
                .thenReturn(Page.empty());
        when(deckRepository.save(any(Deck.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Deck result = deckService().addCard("d1", "sf-1", 1);

        assertThat(result.getCards()).hasSize(1);
        DeckCard added = result.getCards().get(0);
        assertThat(added.getCardName()).isEqualTo("Sol Ring");
        assertThat(added.getColorIdentity()).isEmpty();
        assertThat(added.getInCollection()).isFalse();
        assertThat(added.getIsProxy()).isTrue();
    }

    @Test
    void addCard_marksInCollection_whenLocalHit() {
        Deck deck = sampleDeck();
        MagicCard fetched = MagicCard.builder()
                .scryfallId("sf-1").name("Sol Ring").colorIdentity(List.of())
                .build();
        MagicCard owned = MagicCard.builder().id("mc1").name("Sol Ring").build();
        when(deckRepository.findById("d1")).thenReturn(Optional.of(deck));
        when(catalogClient.findById("sf-1")).thenReturn(fetched);
        when(magicCardRepository.search(any(MagicCardSearchCriteria.class), any(Pageable.class)))
                .thenReturn(new org.springframework.data.domain.PageImpl<>(List.of(owned)));
        when(deckRepository.save(any(Deck.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Deck result = deckService().addCard("d1", "sf-1", 1);

        assertThat(result.getCards().get(0).getInCollection()).isTrue();
        assertThat(result.getCards().get(0).getIsProxy()).isFalse();
    }

    @Test
    void addCard_rejectsInvalidQuantity() {
        assertThatThrownBy(() -> deckService().addCard("d1", "sf-1", 0))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void addCard_throwsNotFound_whenCatalog404() {
        when(deckRepository.findById("d1")).thenReturn(Optional.of(sampleDeck()));
        when(catalogClient.findById("missing"))
                .thenThrow(new HttpClientErrorException(HttpStatus.NOT_FOUND));

        assertThatThrownBy(() -> deckService().addCard("d1", "missing", 1))
                .isInstanceOf(MagicCardNotFoundException.class);
    }

    @Test
    void removeCard_removesByScryfallId() {
        Deck deck = sampleDeck();
        deck.setCards(new java.util.ArrayList<>(List.of(
                DeckCard.builder().cardName("Sol Ring").quantity(1).scryfallId("sf-1").build())));
        when(deckRepository.findById("d1")).thenReturn(Optional.of(deck));
        when(deckRepository.save(any(Deck.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Deck result = deckService().removeCard("d1", "sf-1");

        assertThat(result.getCards()).isEmpty();
    }

    @Test
    void removeCard_throwsBadRequest_whenAbsent() {
        when(deckRepository.findById("d1")).thenReturn(Optional.of(sampleDeck()));

        assertThatThrownBy(() -> deckService().removeCard("d1", "sf-x"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void getStatus_returnsDraft_forNewDeck() {
        when(deckRepository.findById("d1")).thenReturn(Optional.of(sampleDeck()));

        assertThat(deckService().getStatus("d1")).isEqualTo(DeckStatus.DRAFT);
    }

    @Test
    void getStatusReport_returnsEmptyReasons_whenValid() {
        when(deckRepository.findById("d1")).thenReturn(Optional.of(sampleDeck()));

        DeckStatusReport report = deckService().getStatusReport("d1");

        assertThat(report.status()).isEqualTo(DeckStatus.DRAFT);
        assertThat(report.reasons()).isEmpty();
    }

    @Test
    void getStatusReport_returnsReasons_whenInvalid() {
        Deck deck = sampleDeck();
        deck.setCards(new java.util.ArrayList<>(List.of(
                DeckCard.builder().cardName("Black Lotus").quantity(1).scryfallId("sf-x").build())));
        when(deckRepository.findById("d1")).thenReturn(Optional.of(deck));

        DeckStatusReport report = deckService().getStatusReport("d1");

        assertThat(report.status()).isEqualTo(DeckStatus.INVALID);
        assertThat(report.reasons()).anyMatch(r -> r.contains("Black Lotus"));
    }

    @Test
    void getStatusReport_throwsNotFound_whenMissing() {
        when(deckRepository.findById("nope")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> deckService().getStatusReport("nope"))
                .isInstanceOf(DeckNotFoundException.class);
    }

    @Test
    void addCard_persistsDeck() {
        Deck deck = sampleDeck();
        MagicCard fetched = MagicCard.builder()
                .scryfallId("sf-1").name("Sol Ring").colorIdentity(List.of())
                .build();
        when(deckRepository.findById("d1")).thenReturn(Optional.of(deck));
        when(catalogClient.findById("sf-1")).thenReturn(fetched);
        when(magicCardRepository.search(any(MagicCardSearchCriteria.class), any(Pageable.class)))
                .thenReturn(Page.empty());
        when(deckRepository.save(any(Deck.class))).thenAnswer(invocation -> invocation.getArgument(0));

        deckService().addCard("d1", "sf-1", 1);

        ArgumentCaptor<Deck> captor = ArgumentCaptor.forClass(Deck.class);
        verify(deckRepository).save(captor.capture());
        assertThat(captor.getValue().getCards()).hasSize(1);
    }
}
