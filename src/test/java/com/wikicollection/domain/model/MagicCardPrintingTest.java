package com.wikicollection.domain.model;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;

/**
 * Una impresión concreta de una carta. Scryfall distingue la impresión (id) de la
 * carta a través de todas sus reimpresiones (oracle_id): la misma carta se ha
 * impreso cientos de veces y en un mazo de comandante la impresión exacta es parte
 * del producto, así que no basta con devolver "la carta".
 */
class MagicCardPrintingTest {

    @Test
    void guardaLaImpresionConTodosSusDescriptoresDeArte() {
        MagicCardPrinting printing = new MagicCardPrinting(
                "dce15387-2d8d-4f1f-a2b6-c4b2e6b0f1d5",
                "Llanuras",
                "dce",
                "Star Trek",
                "325",
                "mythic",
                "Chris Rahn",
                "2026-08-31",
                "en",
                "https://cards.scryfall.io/normal/front/d/dce15387.jpg",
                "https://cards.scryfall.io/art_crop/front/dce15387.jpg",
                List.of("nonfoil", "foil"),
                true,
                List.of("promo"),
                List.of("inverted"),
                "black",
                "12.34",
                "10.50");

        assertThat(printing.scryfallId()).isEqualTo("dce15387-2d8d-4f1f-a2b6-c4b2e6b0f1d5");
        assertThat(printing.setCode()).isEqualTo("dce");
        assertThat(printing.setName()).isEqualTo("Star Trek");
        assertThat(printing.collectorNumber()).isEqualTo("325");
        assertThat(printing.releasedAt()).isEqualTo("2026-08-31");
        assertThat(printing.lang()).isEqualTo("en");
        assertThat(printing.fullArt()).isTrue();
        assertThat(printing.promoTypes()).containsExactly("promo");
        assertThat(printing.frameEffects()).containsExactly("inverted");
        assertThat(printing.borderColor()).isEqualTo("black");
        assertThat(printing.priceUsd()).isEqualTo("12.34");
        assertThat(printing.priceEur()).isEqualTo("10.50");
    }

    @Test
    void elScryfallIdEsPropioDeCadaImpresion() {
        // Es lo que se pasa a POST /magic/scryfall/{id}: si se repitiera el id de la
        // carta, el usuario volvería a guardar siempre la misma impresión.
        MagicCardPrinting una = new MagicCardPrinting("id-a", "Llanuras", "dce", "Star Trek", "325",
                "mythic", "A", "2026-08-31", "en", null, null, List.of(), false, null, null,
                "black", null, null);
        MagicCardPrinting otra = new MagicCardPrinting("id-b", "Llanuras", "sta", "Strixhaven", "12",
                "mythic", "B", "2021-04-23", "en", null, null, List.of(), false, null, null,
                "black", null, null);

        assertThat(una.scryfallId()).isNotEqualTo(otra.scryfallId());
    }

    @Test
    void fullArtEsPrimitivoParaQueAusenteSignifiqueFalso() {
        // Scryfall omite full_art en las cartas antiguas. Con Boolean el frontend
        // tendría que tratar null y false como cosas distintas; no lo son.
        MagicCardPrinting sinCampo = new MagicCardPrinting("id-a", "Llanuras", "dce", "Star Trek", "325",
                "mythic", "A", "2026-08-31", "en", null, null, null, false, null, null, null,
                null, null);

        assertThat(sinCampo.fullArt()).isFalse();
    }
}
