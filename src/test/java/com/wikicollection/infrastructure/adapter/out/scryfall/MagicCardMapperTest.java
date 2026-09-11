package com.wikicollection.infrastructure.adapter.out.scryfall;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import com.wikicollection.domain.model.MagicCard;
import com.wikicollection.domain.model.MagicCardSearchResult;
import com.wikicollection.infrastructure.adapter.out.scryfall.MagicCardMapper.ScryfallCardResponse;
import com.wikicollection.infrastructure.adapter.out.scryfall.MagicCardMapper.ScryfallImageUris;
import com.wikicollection.infrastructure.adapter.out.scryfall.MagicCardMapper.ScryfallListResponse;
import com.wikicollection.infrastructure.adapter.out.scryfall.MagicCardMapper.ScryfallPrices;

import org.junit.jupiter.api.Test;

class MagicCardMapperTest {

    private final MagicCardMapper mapper = new MagicCardMapper();

    @Test
    void mapResponse_mapsEachCardToSearchResult() {
        ScryfallCardResponse card = new ScryfallCardResponse(
                "id-1", "oracle-1", "Lightning Bolt", "2026-06-26", "{R}", 1.0,
                "Instant", "texto", "1", "1", null,
                List.of("R"), List.of("R"), List.of(), "uncommon",
                "msc", "Marvel Super Heroes Commander", "Milivoj", "2015", "black",
                "normal", java.util.Map.of(), new ScryfallPrices("0.65", "2.02"),
                new ScryfallImageUris("http://normal", "http://large", "http://crop"));

        List<MagicCardSearchResult> results = mapper.mapResponse(new ScryfallListResponse(List.of(card)));

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
    void mapResponse_returnsEmpty_whenNullList() {
        assertThat(mapper.mapResponse(new ScryfallListResponse(null))).isEmpty();
        assertThat(mapper.mapResponse(null)).isEmpty();
    }

    @Test
    void map_mapsAllFieldsToDomain() {
        ScryfallCardResponse card = new ScryfallCardResponse(
                "id-1", "oracle-1", "Lightning Bolt", "2026-06-26", "{R}", 1.0,
                "Instant", "texto", "1", "1", "2",
                List.of("R"), List.of("R"), List.of("Flash"), "uncommon",
                "msc", "Marvel Super Heroes Commander", "Milivoj", "2015", "black",
                "normal", java.util.Map.of("standard", "legal"),
                new ScryfallPrices("0.65", "2.02"),
                new ScryfallImageUris("http://normal", "http://large", "http://crop"));

        MagicCard cardDomain = mapper.map(card);

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
    void map_handlesMissingOptionalFields() {
        ScryfallCardResponse card = new ScryfallCardResponse(
                "id-1", null, "Lightning Bolt",
                null, null, null, null, null, null, null, null,
                null, null, null, null, null, null, null, null, null, null,
                null, null,
                new ScryfallImageUris(null, null, null));

        MagicCard cardDomain = mapper.map(card);

        assertThat(cardDomain.getName()).isEqualTo("Lightning Bolt");
        assertThat(cardDomain.getPriceUsd()).isNull();
        assertThat(cardDomain.getImageUrl()).isNull();
        assertThat(cardDomain.getColors()).isNull();
    }
}
