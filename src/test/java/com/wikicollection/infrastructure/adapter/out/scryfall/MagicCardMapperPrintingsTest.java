package com.wikicollection.infrastructure.adapter.out.scryfall;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.wikicollection.domain.model.MagicCardPrinting;

import org.junit.jupiter.api.Test;

/**
 * Usa JSON real de Scryfall en lugar del constructor posicional de 31 campos: lo que
 * importa aquí es que los nombres de campo coincidan con los que manda la API, y un
 * {@code @JsonProperty} mal escrito daría null en silencio.
 *
 * <p>Fixture verificado contra {@code /cards/search?unique=prints} (2026-10-02).
 */
class MagicCardMapperPrintingsTest {

    private final MagicCardMapper mapper = new MagicCardMapper();
    private final ObjectMapper objectMapper = new ObjectMapper();

    private MagicCardMapper.ScryfallListResponse parse(String json) throws Exception {
        return objectMapper.readValue(json, MagicCardMapper.ScryfallListResponse.class);
    }

    /** Envuelve una carta suelta en la forma de lista que devuelve /cards/search. */
    private MagicCardMapper.ScryfallCardResponse parseCard(String cardJson) throws Exception {
        return parse("{\"data\": [" + cardJson + "]}").data().get(0);
    }

    private static final String PRINTING_JSON = """
            {
              "object": "card",
              "id": "dce15387-2d8d-4f1f-a2b6-c4b2e6b0f1d5",
              "oracle_id": "b34bb2dc-1f5e-4c8e-9c2f-4a3f0b9d7e21",
              "name": "Llanuras",
              "lang": "en",
              "released_at": "2026-08-31",
              "mana_cost": "{3}{W}",
              "cmc": 4.0,
              "type_line": "Legendary Land",
              "rarity": "mythic",
              "set": "dce",
              "set_name": "Star Trek",
              "collector_number": "325",
              "artist": "Chris Rahn",
              "border_color": "black",
              "frame": "2026",
              "full_art": true,
              "finishes": ["nonfoil", "foil"],
              "promo_types": ["promo", "showcase"],
              "frame_effects": ["inverted"],
              "prices": { "usd": "12.34", "eur": "10.50", "usd_foil": "99.99" },
              "image_uris": {
                "normal": "https://cards.scryfall.io/normal/front/dce15387.jpg",
                "large": "https://cards.scatterfallfront/large/front/dce15387.jpg",
                "art_crop": "https://cards.scryfall.io/art_crop/front/dce15387.jpg"
              }
            }
            """;

    /** Genera n impresiones con la forma que devuelve Scryfall en la lista. */
    private static String impressions(int n) {
        StringBuilder data = new StringBuilder();
        for (int i = 0; i < n; i++) {
            if (i > 0) {
                data.append(',');
            }
            data.append("{\"object\":\"card\",\"id\":\"id-").append(i).append("\",\"name\":\"Llanuras\"}");
        }
        return data.toString();
    }

    @Test
    void ligaLosDescriptoresDeArteDelJsonReal() throws Exception {
        MagicCardPrinting printing = mapper.toPrinting(parseCard(PRINTING_JSON));

        assertThat(printing.scryfallId()).isEqualTo("dce15387-2d8d-4f1f-a2b6-c4b2e6b0f1d5");
        assertThat(printing.name()).isEqualTo("Llanuras");
        assertThat(printing.setCode()).isEqualTo("dce");
        assertThat(printing.setName()).isEqualTo("Star Trek");
        assertThat(printing.collectorNumber()).isEqualTo("325");
        assertThat(printing.rarity()).isEqualTo("mythic");
        assertThat(printing.artist()).isEqualTo("Chris Rahn");
        assertThat(printing.releasedAt()).isEqualTo("2026-08-31");
        assertThat(printing.lang()).isEqualTo("en");
        assertThat(printing.borderColor()).isEqualTo("black");
        assertThat(printing.finishes()).containsExactly("nonfoil", "foil");
        assertThat(printing.promoTypes()).containsExactly("promo", "showcase");
        assertThat(printing.frameEffects()).containsExactly("inverted");
        assertThat(printing.fullArt()).isTrue();
        assertThat(printing.imageUrl()).isEqualTo("https://cards.scryfall.io/normal/front/dce15387.jpg");
        assertThat(printing.artCropUrl()).isEqualTo("https://cards.scryfall.io/art_crop/front/dce15387.jpg");
    }

    @Test
    void tomaLosPreciosSimplesYDescartaLosDeAcabadoEspecial() throws Exception {
        MagicCardPrinting printing = mapper.toPrinting(parseCard(PRINTING_JSON));

        assertThat(printing.priceUsd()).isEqualTo("12.34");
        assertThat(printing.priceEur()).isEqualTo("10.50");
    }

    @Test
    void unaCartaAntiguaSinLosDescriptoresDeArteNoRevienta() throws Exception {
        // Scryfall omite full_art, finishes, promo_types y frame_effects en las cartas antiguas.
        String antigua = """
                { "object": "card", "id": "id-a", "oracle_id": "oracle-a", "name": "Llanuras",
                  "lang": "en", "released_at": "1993-08-05", "rarity": "rare", "set": "ice",
                  "set_name": "Ice Age", "border_color": "black" }
                """;

        MagicCardPrinting printing = mapper.toPrinting(parseCard(antigua));

        assertThat(printing.fullArt()).isFalse();
        assertThat(printing.finishes()).isNull();
        assertThat(printing.promoTypes()).isNull();
        assertThat(printing.frameEffects()).isNull();
        assertThat(printing.priceUsd()).isNull();
        assertThat(printing.priceEur()).isNull();
        assertThat(printing.imageUrl()).isNull();
        assertThat(printing.artCropUrl()).isNull();
        assertThat(printing.collectorNumber()).isNull();
    }

    @Test
    void elScryfallIdEsPropioDeCadaImpresionDeLaMismaCarta() throws Exception {
        String dosImpresiones = """
                { "total_cards": 955, "data": [
                  { "object": "card", "id": "id-a", "oracle_id": "oracle-x", "name": "Llanuras",
                    "set": "dce", "released_at": "2026-08-31" },
                  { "object": "card", "id": "id-b", "oracle_id": "oracle-x", "name": "Llanuras",
                    "set": "sta", "released_at": "2021-04-23" }
                ] }
                """;

        var page = mapper.mapPrintings(parse(dosImpresiones), 0);

        assertThat(page.getContent()).extracting(MagicCardPrinting::scryfallId)
                .containsExactly("id-a", "id-b");
    }

    @Test
    void mapeaUnaPaginaConElTotalRealYLasPaginasQueImplican() throws Exception {
        var page = mapper.mapPrintings(parse("{\"total_cards\": 955, \"data\": [" + impressions(175) + "]}"), 0);

        // 955 impresiones con paginas fijas de 175 son 6 paginas, no las 48 que daria size=20.
        assertThat(page.getContent()).hasSize(175);
        assertThat(page.getTotalElements()).isEqualTo(955);
        assertThat(page.getTotalPages()).isEqualTo(6);
        assertThat(page.getSize()).isEqualTo(MagicCardPrinting.PAGE_SIZE);
        assertThat(page.getNumber()).isZero();
    }

    @Test
    void elTotalAusenteSeRellenaConElTamanoDeLaPagina() throws Exception {
        String sinTotal = """
                { "data": [ { "object": "card", "id": "id-a", "name": "Llanuras" } ] }
                """;

        var page = mapper.mapPrintings(parse(sinTotal), 0);

        assertThat(page.getTotalElements()).isEqualTo(1);
    }

    @Test
    void unaCartaConMenosImpresionesQueElTamanoDePaginaCabeEnUna() throws Exception {
        // Scryfall devuelve los 100 en una sola pagina: total y contenido coinciden.
        var page = mapper.mapPrintings(parse("{\"total_cards\": 100, \"data\": [" + impressions(100) + "]}"), 0);

        assertThat(page.getContent()).hasSize(100);
        assertThat(page.getTotalElements()).isEqualTo(100);
        assertThat(page.getTotalPages()).isEqualTo(1);
    }

    /**
     * Spring Data {@code PageImpl} reescribe el total a {@code offset + content.size()}
     * cuando la pagina trae contenido y {@code offset + pageSize > total}. Con pageSize
     * fijo en 175 eso solo ocurre en la ultima pagina parcial, donde el recorte da
     * justo el total real. Se fija aqui para que un cambio en el mapper no lo rompa en
     * silencio.
     */
    @Test
    void laUltimaPaginaParcialReportaElTotalRealYNoElTamanoDePagina() throws Exception {
        // 200 impresiones con paginas de 175: la pagina 1 trae las 25 que quedan.
        String ultima = "{\"total_cards\": 200, \"data\": [" + impressions(25) + "]}";

        // offset = 175, pageSize = 175 -> 350 > 200, asi que PageImpl recalcula.
        var page = mapper.mapPrintings(parse(ultima), 1);

        assertThat(page.getContent()).hasSize(25);
        assertThat(page.getTotalElements()).isEqualTo(200);
        assertThat(page.getTotalPages()).isEqualTo(2);
        assertThat(page.getNumber()).isEqualTo(1);
    }

    @Test
    void unaPaginaVaciaPosteriorALaUltimaConservaElTotalReal() {
        // PageImpl solo recorta el total si la pagina trae contenido; una pagina vacia
        // posterior a la ultima conserva el total que dijo Scryfall.
        var page = mapper.mapPrintings(new MagicCardMapper.ScryfallListResponse(List.of(), 100L), 1);

        assertThat(page.getContent()).isEmpty();
        assertThat(page.getTotalElements()).isEqualTo(100);
        assertThat(page.getTotalPages()).isEqualTo(1);
    }

    @Test
    void unaPaginaVaciaEsVaciaYNoRevienta() throws Exception {
        String vacia = """
                { "total_cards": 0, "data": [] }
                """;

        var page = mapper.mapPrintings(parse(vacia), 0);

        assertThat(page.getContent()).isEmpty();
        assertThat(page.getTotalElements()).isZero();
        assertThat(page.getTotalPages()).isZero();
    }

    @Test
    void unaRespuestaNullProduceUnaPaginaVacia() {
        assertThat(mapper.mapPrintings(null, 0).getContent()).isEmpty();
        assertThat(mapper.mapPrintings(null, 0).getTotalElements()).isZero();
        assertThat(mapper.mapPrintings(null, 0).getTotalPages()).isZero();
    }

    @Test
    void unaPaginaNegativaSeTrataComoLaPrimera() {
        var page = mapper.mapPrintings(new MagicCardMapper.ScryfallListResponse(List.of(), 0L), -1);

        assertThat(page.getNumber()).isZero();
    }
}
