package com.wikicollection.application.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;

import com.wikicollection.domain.model.Deck;
import com.wikicollection.domain.model.DeckCard;
import com.wikicollection.domain.model.DeckStatus;

import org.junit.jupiter.api.Test;

class DeckValidationTest {

    private final DeckValidator validator = new DeckValidator();

    private DeckCard card(String scryfallId, String name, int quantity, List<String> colors) {
        return DeckCard.builder()
                .cardName(name)
                .quantity(quantity)
                .typeLine("Creature")
                .colorIdentity(colors)
                .scryfallId(scryfallId)
                .build();
    }

    private Deck draftDeck(List<DeckCard> cards) {
        return Deck.builder()
                .id("d1")
                .name("Mazo")
                .commander("Atraxa, Praetors' Voice")
                .commanderColors(List.of("W", "U", "B", "G"))
                .cards(new ArrayList<>(cards))
                .build();
    }

    @Test
    void validate_emptyDeck_hasNoViolations() {
        assertThat(validator.validate(draftDeck(List.of()))).isEmpty();
    }

    @Test
    void validate_nullDeck_reportsViolation() {
        assertThat(validator.validate(null)).isNotEmpty();
        assertThat(validator.evaluate(null)).isEqualTo(DeckStatus.INVALID);
    }

    @Test
    void validate_rejectsInvalidQuantity() {
        Deck deck = draftDeck(List.of(card("sf-1", "Sol Ring", 0, List.of())));

        assertThat(validator.validate(deck)).anyMatch(v -> v.contains("Sol Ring"));
    }

    @Test
    void validate_rejectsDuplicateCard_singleton() {
        Deck deck = draftDeck(List.of(
                card("sf-1", "Sol Ring", 1, List.of()),
                card("sf-1", "Sol Ring", 1, List.of())));

        assertThat(validator.validate(deck)).anyMatch(v -> v.contains("singleton"));
    }

    @Test
    void validate_rejectsQuantityAboveOne_forNonBasic() {
        Deck deck = draftDeck(List.of(card("sf-1", "Sol Ring", 2, List.of())));

        assertThat(validator.validate(deck)).anyMatch(v -> v.contains("singleton"));
    }

    @Test
    void validate_allowsMultipleBasicLands() {
        Deck deck = draftDeck(List.of(
                DeckCard.builder().cardName("Forest").quantity(10).typeLine("Basic Land — Forest")
                        .colorIdentity(List.of("G")).scryfallId("forest").build()));

        assertThat(validator.validate(deck)).isEmpty();
    }

    @Test
    void validate_rejectsCardOutsideCommanderColors() {
        Deck deck = draftDeck(List.of(card("sf-1", "Lightning Bolt", 1, List.of("R"))));

        assertThat(validator.validate(deck)).anyMatch(v -> v.contains("identidad de color"));
    }

    @Test
    void validate_acceptsCardInsideCommanderColors() {
        Deck deck = draftDeck(List.of(card("sf-1", "Swords to Plowshares", 1, List.of("W"))));

        assertThat(validator.validate(deck)).isEmpty();
    }

    @Test
    void validate_rejectsBannedCard() {
        Deck deck = draftDeck(List.of(card("sf-x", "Black Lotus", 1, List.of())));

        assertThat(validator.validate(deck)).anyMatch(v -> v.contains("baneada"));
    }

    @Test
    void validate_rejectsDeckAbove100Cards() {
        List<DeckCard> cards = IntStream.range(0, 100)
                .mapToObj(i -> card("sf-" + i, "Carta " + i, 1, List.of("W")))
                .toList();
        Deck deck = draftDeck(cards);

        assertThat(validator.validate(deck)).anyMatch(v -> v.contains("100 cartas"));
        assertThat(validator.evaluate(deck)).isEqualTo(DeckStatus.INVALID);
    }

    @Test
    void evaluate_complete_when99PlusCommander() {
        List<DeckCard> cards = IntStream.range(0, 99)
                .mapToObj(i -> card("sf-" + i, "Carta " + i, 1, List.of("W")))
                .toList();
        Deck deck = draftDeck(cards);

        assertThat(validator.validate(deck)).isEmpty();
        assertThat(validator.evaluate(deck)).isEqualTo(DeckStatus.COMPLETE);
    }

    @Test
    void evaluate_draft_whenIncomplete() {
        assertThat(validator.evaluate(draftDeck(List.of()))).isEqualTo(DeckStatus.DRAFT);
    }

    @Test
    void evaluate_draft_whenNoCommander() {
        Deck deck = Deck.builder().name("Sin comandante").cards(List.of()).build();

        assertThat(validator.evaluate(deck)).isEqualTo(DeckStatus.DRAFT);
    }
}
