package com.wikicollection.infrastructure.adapter.in.web.dto;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;
import java.util.List;

import com.wikicollection.domain.model.Deck;
import com.wikicollection.domain.model.DeckCard;
import com.wikicollection.domain.model.DeckStatus;
import com.wikicollection.domain.model.DeckStatusReport;

import org.junit.jupiter.api.Test;

class DeckDtoMapperTest {

    private final DeckDtoMapper mapper = new DeckDtoMapper();

    @Test
    void toDomain_mapsRequestFields() {
        DeckRequest request = new DeckRequest("Mi Commander", "Desc",
                "Atraxa, Praetors' Voice", List.of("W", "U", "B", "G"));

        Deck deck = mapper.toDomain(request);

        assertThat(deck.getName()).isEqualTo("Mi Commander");
        assertThat(deck.getDescription()).isEqualTo("Desc");
        assertThat(deck.getCommander()).isEqualTo("Atraxa, Praetors' Voice");
        assertThat(deck.getCommanderColors()).containsExactly("W", "U", "B", "G");
    }

    @Test
    void toResponse_mapsDeckWithCards() {
        Deck deck = Deck.builder()
                .id("d1")
                .name("Mi Commander")
                .commander("Atraxa, Praetors' Voice")
                .commanderColors(List.of("W", "U", "B", "G"))
                .cards(List.of(DeckCard.builder()
                        .cardName("Sol Ring")
                        .quantity(1)
                        .inCollection(true)
                        .isProxy(false)
                        .colorIdentity(List.of())
                        .scryfallId("sf-1")
                        .build()))
                .createdAt(LocalDateTime.of(2026, 1, 1, 12, 0))
                .updatedAt(LocalDateTime.of(2026, 1, 2, 12, 0))
                .build();

        DeckResponse response = mapper.toResponse(deck);

        assertThat(response.id()).isEqualTo("d1");
        assertThat(response.name()).isEqualTo("Mi Commander");
        assertThat(response.cards()).hasSize(1);
        assertThat(response.cards().get(0).cardName()).isEqualTo("Sol Ring");
        assertThat(response.cards().get(0).scryfallId()).isEqualTo("sf-1");
        assertThat(response.createdAt()).isEqualTo(LocalDateTime.of(2026, 1, 1, 12, 0));
    }

    @Test
    void toResponse_mapsNullCardsToEmpty() {
        Deck deck = Deck.builder().id("d1").name("Vacio").build();

        assertThat(mapper.toResponse(deck).cards()).isEmpty();
    }

    @Test
    void toStatusResponse_mapsInvalidWithJoinedMessage() {
        DeckStatusReport report = new DeckStatusReport(
                DeckStatus.INVALID, List.of("Razón uno", "Razón dos"));

        DeckStatusResponse response = mapper.toStatusResponse(report);

        assertThat(response.status()).isEqualTo(DeckStatus.INVALID);
        assertThat(response.message()).isEqualTo("Razón uno; Razón dos");
    }

    @Test
    void toStatusResponse_mapsValidWithNullMessage() {
        DeckStatusResponse response = mapper.toStatusResponse(new DeckStatusReport(DeckStatus.DRAFT, List.of()));

        assertThat(response.status()).isEqualTo(DeckStatus.DRAFT);
        assertThat(response.message()).isNull();
    }

    @Test
    void toStatusResponse_returnsNull_whenNull() {
        assertThat(mapper.toStatusResponse(null)).isNull();
    }

    @Test
    void mappers_returnNull_whenNull() {
        assertThat(mapper.toDomain(null)).isNull();
        assertThat(mapper.toResponse(null)).isNull();
    }
}
