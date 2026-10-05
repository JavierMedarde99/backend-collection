package com.wikicollection.application.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import com.wikicollection.domain.model.DeckCard;
import com.wikicollection.domain.model.MagicCard;
import com.wikicollection.domain.model.MagicCardSearchResult;

import org.junit.jupiter.api.Test;

/**
 * El mapeo a {@code DeckCard} vive en un sitio compartido porque lo necesitan dos caminos
 * distintos: {@code DeckService.addCard} (que parte de una {@code MagicCard} guardada) y la
 * importación de mazos (#353), que parte del resultado de buscar el nombre en Scryfall y no
 * guarda la carta.
 */
class DeckCardFactoryTest {

    private final DeckCardFactory factory = new DeckCardFactory();

    @Test
    void fromMagicCard_mapsEveryField() {
        MagicCard card = MagicCard.builder()
                .name("Sol Ring")
                .manaCost("{1}")
                .type("Artifact")
                .colorIdentity(List.of("G"))
                .imageUrl("https://img")
                .scryfallId("abc")
                .build();

        DeckCard result = factory.fromMagicCard(card, 4, true);

        assertThat(result.getCardName()).isEqualTo("Sol Ring");
        assertThat(result.getQuantity()).isEqualTo(4);
        assertThat(result.getInCollection()).isTrue();
        assertThat(result.getIsProxy()).isFalse();
        assertThat(result.getManaCost()).isEqualTo("{1}");
        assertThat(result.getTypeLine()).isEqualTo("Artifact");
        assertThat(result.getColorIdentity()).containsExactly("G");
        assertThat(result.getImageUrl()).isEqualTo("https://img");
        assertThat(result.getScryfallId()).isEqualTo("abc");
    }

    @Test
    void fromMagicCard_notInCollection_marksProxy() {
        MagicCard card = MagicCard.builder().name("Sol Ring").scryfallId("abc").build();

        DeckCard result = factory.fromMagicCard(card, 1, false);

        assertThat(result.getInCollection()).isFalse();
        assertThat(result.getIsProxy()).isTrue();
    }

    @Test
    void fromSearchResult_mapsEveryField() {
        MagicCardSearchResult card = searchResult();

        DeckCard result = factory.fromSearchResult(card, 2, true);

        assertThat(result.getCardName()).isEqualTo("Sol Ring");
        assertThat(result.getQuantity()).isEqualTo(2);
        assertThat(result.getInCollection()).isTrue();
        assertThat(result.getIsProxy()).isFalse();
        assertThat(result.getManaCost()).isEqualTo("{1}");
        assertThat(result.getTypeLine()).isEqualTo("Artifact");
        assertThat(result.getColorIdentity()).containsExactly("G");
        assertThat(result.getImageUrl()).isEqualTo("https://img");
        assertThat(result.getScryfallId()).isEqualTo("abc");
    }

    @Test
    void fromSearchResult_notInCollection_marksProxy() {
        DeckCard result = factory.fromSearchResult(searchResult(), 1, false);

        assertThat(result.getInCollection()).isFalse();
        assertThat(result.getIsProxy()).isTrue();
    }

    private MagicCardSearchResult searchResult() {
        return new MagicCardSearchResult("abc", "Sol Ring", "{1}", "Artifact", "rare", "neo",
                "Neon Dynasty", "https://img", "1.25", List.of(), List.of("G"), "text");
    }
}