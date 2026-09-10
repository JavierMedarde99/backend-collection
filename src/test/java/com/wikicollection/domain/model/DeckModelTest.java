package com.wikicollection.domain.model;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;

class DeckModelTest {

    @Test
    void deck_buildsWithAllFields() {
        DeckCard card = DeckCard.builder()
                .cardName("Sol Ring")
                .quantity(1)
                .inCollection(true)
                .isProxy(false)
                .manaCost("{1}")
                .typeLine("Artifact")
                .colorIdentity(List.of())
                .imageUrl("http://img")
                .scryfallId("sf-1")
                .build();

        Deck deck = Deck.builder()
                .id("d1")
                .name("Mi Commander")
                .description("Mazo de prueba")
                .commander("Atraxa, Praetors' Voice")
                .commanderColors(List.of("W", "U", "B", "G"))
                .cards(List.of(card))
                .createdAt(LocalDateTime.of(2026, 1, 1, 12, 0))
                .updatedAt(LocalDateTime.of(2026, 1, 2, 12, 0))
                .build();

        assertThat(deck.getId()).isEqualTo("d1");
        assertThat(deck.getName()).isEqualTo("Mi Commander");
        assertThat(deck.getDescription()).isEqualTo("Mazo de prueba");
        assertThat(deck.getCommander()).isEqualTo("Atraxa, Praetors' Voice");
        assertThat(deck.getCommanderColors()).containsExactly("W", "U", "B", "G");
        assertThat(deck.getCards()).hasSize(1);
        assertThat(deck.getCards().get(0).getCardName()).isEqualTo("Sol Ring");
        assertThat(deck.getCards().get(0).getColorIdentity()).isEmpty();
        assertThat(deck.getCreatedAt()).isEqualTo(LocalDateTime.of(2026, 1, 1, 12, 0));
        assertThat(deck.getUpdatedAt()).isEqualTo(LocalDateTime.of(2026, 1, 2, 12, 0));
    }

    @Test
    void deckStatus_hasExpectedValues() {
        assertThat(DeckStatus.values()).containsExactly(DeckStatus.DRAFT, DeckStatus.COMPLETE, DeckStatus.INVALID);
    }
}
