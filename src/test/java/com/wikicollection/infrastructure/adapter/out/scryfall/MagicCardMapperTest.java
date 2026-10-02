package com.wikicollection.infrastructure.adapter.out.scryfall;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.wikicollection.domain.model.MagicCard;
import com.wikicollection.domain.model.MagicCardSearchResult;
import com.wikicollection.infrastructure.adapter.out.scryfall.MagicCardMapper.ScryfallCardResponse;
import com.wikicollection.infrastructure.adapter.out.scryfall.MagicCardMapper.ScryfallListResponse;

import org.junit.jupiter.api.Test;

/**
 * Construye las respuestas desde JSON, no con el constructor posicional: los registros de
 * Scryfall solo tienen el constructor canónico, porque añadir un segundo construct hizo que
 * Jackson eligiese entre ellos según su configuración global y perdiera campos como
 * total_cards solo en el contexto de Spring.
 */
class MagicCardMapperTest {

    private final MagicCardMapper mapper = new MagicCardMapper();
    private final ObjectMapper objectMapper = new ObjectMapper();

    private ScryfallCardResponse card(String json) throws Exception {
        return objectMapper.readValue(json, ScryfallCardResponse.class);
    }

    private ScryfallListResponse list(String data) throws Exception {
        return objectMapper.readValue("{\"data\": [" + data + "], \"total_cards\": 1}",
                ScryfallListResponse.class);
    }

    private static final String LIGHTNING_BOLT = """
            { "id": "id-1", "oracle_id": "oracle-1", "name": "Lightning Bolt",
              "released_at": "2026-06-26", "mana_cost": "{R}", "cmc": 1.0,
              "type_line": "Instant", "oracle_text": "texto",
              "power": "1", "toughness": "1", "loyalty": "2",
              "colors": ["R"], "color_identity": ["R"], "keywords": ["Flash"],
              "rarity": "uncommon", "set": "msc", "set_name": "Marvel Super Heroes Commander",
              "artist": "Milivoj", "frame": "2015", "border_color": "black", "layout": "normal",
              "legalities": { "standard": "legal" },
              "prices": { "usd": "0.65", "eur": "2.02" },
              "image_uris": { "normal": "http://normal", "large": "http://large",
                              "art_crop": "http://crop" } }
            """;

    private static String boltWithoutOptionals() {
        return """
                { "id": "id-1", "name": "Lightning Bolt" }
                """;
    }

    @Test
    void mapResponse_mapsEachCardToSearchResult() throws Exception {
        List<MagicCardSearchResult> results = mapper.mapResponse(list(LIGHTNING_BOLT));

        assertThat(results).hasSize(1);
        MagicCardSearchResult result = results.get(0);
        assertThat(result.scryfallId()).isEqualTo("id-1");
        assertThat(result.name()).isEqualTo("Lightning Bolt");
        assertThat(result.manaCost()).isEqualTo("{R}");
        assertThat(result.type()).isEqualTo("Instant");
        assertThat(result.rarity()).isEqualTo("uncommon");
        assertThat(result.setCode()).isEqualTo("msc");
        assertThat(result.setName()).isEqualTo("Marvel Super Heroes Commander");
        assertThat(result.imageUrl()).isEqualTo("http://normal");
        assertThat(result.priceUsd()).isEqualTo("0.65");
        assertThat(result.colors()).containsExactly("R");
        assertThat(result.colorIdentity()).containsExactly("R");
        assertThat(result.text()).isEqualTo("texto");
    }

    @Test
    void mapResponse_returnsEmpty_whenNullList() throws Exception {
        assertThat(mapper.mapResponse(new ScryfallListResponse(null, null))).isEmpty();
        assertThat(mapper.mapResponse(null)).isEmpty();
        assertThat(mapper.mapResponse(objectMapper.readValue("{}", ScryfallListResponse.class))).isEmpty();
    }

    @Test
    void map_mapsAllFieldsToDomain() throws Exception {
        MagicCard cardDomain = mapper.map(card(LIGHTNING_BOLT));

        assertThat(cardDomain.getScryfallId()).isEqualTo("id-1");
        assertThat(cardDomain.getOracleId()).isEqualTo("oracle-1");
        assertThat(cardDomain.getName()).isEqualTo("Lightning Bolt");
        assertThat(cardDomain.getReleaseDate()).isEqualTo("2026-06-26");
        assertThat(cardDomain.getManaCost()).isEqualTo("{R}");
        assertThat(cardDomain.getConvertedManaCost()).isEqualTo(1.0);
        assertThat(cardDomain.getType()).isEqualTo("Instant");
        assertThat(cardDomain.getText()).isEqualTo("texto");
        assertThat(cardDomain.getPower()).isEqualTo("1");
        assertThat(cardDomain.getToughness()).isEqualTo("1");
        assertThat(cardDomain.getLoyalty()).isEqualTo("2");
        assertThat(cardDomain.getColors()).containsExactly("R");
        assertThat(cardDomain.getColorIdentity()).containsExactly("R");
        assertThat(cardDomain.getKeywords()).containsExactly("Flash");
        assertThat(cardDomain.getRarity()).isEqualTo("uncommon");
        assertThat(cardDomain.getSetCode()).isEqualTo("msc");
        assertThat(cardDomain.getSetName()).isEqualTo("Marvel Super Heroes Commander");
        assertThat(cardDomain.getArtist()).isEqualTo("Milivoj");
        assertThat(cardDomain.getFrame()).isEqualTo("2015");
        assertThat(cardDomain.getBorderColor()).isEqualTo("black");
        assertThat(cardDomain.getLayout()).isEqualTo("normal");
        assertThat(cardDomain.getLegalities()).containsEntry("standard", "legal");
        assertThat(cardDomain.getPriceUsd()).isEqualTo("0.65");
        assertThat(cardDomain.getPriceEur()).isEqualTo("2.02");
        assertThat(cardDomain.getImageUrl()).isEqualTo("http://normal");
        assertThat(cardDomain.getImageLargeUrl()).isEqualTo("http://large");
        assertThat(cardDomain.getArtCropUrl()).isEqualTo("http://crop");
    }

    @Test
    void map_returnsNull_whenNull() {
        assertThat(mapper.map((ScryfallCardResponse) null)).isNull();
    }

    @Test
    void map_handlesMissingOptionalFields() throws Exception {
        MagicCard cardDomain = mapper.map(card(boltWithoutOptionals()));

        assertThat(cardDomain.getName()).isEqualTo("Lightning Bolt");
        assertThat(cardDomain.getPriceUsd()).isNull();
        assertThat(cardDomain.getImageUrl()).isNull();
        assertThat(cardDomain.getColors()).isNull();
        assertThat(cardDomain.getLegalities()).isNull();
    }

    @Test
    void losPreciosQueScryfallMandaDeMasNoRompenLaDeserializacion() throws Exception {
        // Scryfall manda usd_foil, usd_etched, etc. Si el anidado no los ignorase, la
        // deserialización dependería de la configuración global de Jackson.
        String conPrecios = """
                { "id": "id-1", "prices": { "usd": "1.00", "usd_foil": "9.99",
                                            "usd_etched": null, "eur": "0.90", "eur_foil": null } }
                """;

        assertThat(mapper.map(card(conPrecios)).getPriceUsd()).isEqualTo("1.00");
    }

    @Test
    void losCamposDeLegalitiesSeMapeanComoMapa() throws Exception {
        String conLegalities = """
                { "id": "id-1", "legalities": { "standard": "legal", "commander": "banned" } }
                """;

        assertThat(mapper.map(card(conLegalities)).getLegalities())
                .isEqualTo(Map.of("standard", "legal", "commander", "banned"));
    }
}