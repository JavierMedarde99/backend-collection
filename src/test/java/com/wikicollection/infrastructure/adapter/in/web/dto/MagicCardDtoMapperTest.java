package com.wikicollection.infrastructure.adapter.in.web.dto;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import com.wikicollection.domain.model.MagicCard;
import com.wikicollection.domain.model.MagicCardCondition;
import com.wikicollection.domain.model.MagicCardLanguage;

import org.junit.jupiter.api.Test;

class MagicCardDtoMapperTest {

    private final MagicCardDtoMapper mapper = new MagicCardDtoMapper();

    @Test
    void toResponse_mapsAllDomainFields() {
        MagicCard card = MagicCard.builder()
                .id("mc1")
                .scryfallId("s1")
                .oracleId("o1")
                .name("Lightning Bolt")
                .language(MagicCardLanguage.SPANISH)
                .releaseDate("2026-06-26")
                .manaCost("{R}")
                .convertedManaCost(1.0)
                .type("Instant")
                .text("texto")
                .power("1")
                .toughness("1")
                .loyalty("2")
                .colors(List.of("R"))
                .colorIdentity(List.of("R"))
                .keywords(List.of("Flash"))
                .rarity("uncommon")
                .setCode("msc")
                .setName("Marvel Super Heroes Commander")
                .artist("Milivoj")
                .frame("2015")
                .borderColor("black")
                .layout("normal")
                .legalities(Map.of("standard", "legal"))
                .priceUsd("0.65")
                .priceEur("2.02")
                .imageUrl("http://img")
                .imageLargeUrl("http://large")
                .artCropUrl("http://crop")
                .condition(MagicCardCondition.GOOD)
                .isFoil(false)
                .quantity(2)
                .notes("Nota")
                .dateAdded(LocalDateTime.of(2026, 1, 1, 12, 0))
                .build();

        MagicCardResponse response = mapper.toResponse(card);

        assertThat(response.id()).isEqualTo("mc1");
        assertThat(response.scryfallId()).isEqualTo("s1");
        assertThat(response.oracleId()).isEqualTo("o1");
        assertThat(response.name()).isEqualTo("Lightning Bolt");
        assertThat(response.language()).isEqualTo(MagicCardLanguage.SPANISH);
        assertThat(response.releaseDate()).isEqualTo("2026-06-26");
        assertThat(response.manaCost()).isEqualTo("{R}");
        assertThat(response.convertedManaCost()).isEqualTo(1.0);
        assertThat(response.type()).isEqualTo("Instant");
        assertThat(response.text()).isEqualTo("texto");
        assertThat(response.power()).isEqualTo("1");
        assertThat(response.toughness()).isEqualTo("1");
        assertThat(response.loyalty()).isEqualTo("2");
        assertThat(response.colors()).containsExactly("R");
        assertThat(response.colorIdentity()).containsExactly("R");
        assertThat(response.keywords()).containsExactly("Flash");
        assertThat(response.rarity()).isEqualTo("uncommon");
        assertThat(response.setCode()).isEqualTo("msc");
        assertThat(response.setName()).isEqualTo("Marvel Super Heroes Commander");
        assertThat(response.artist()).isEqualTo("Milivoj");
        assertThat(response.frame()).isEqualTo("2015");
        assertThat(response.borderColor()).isEqualTo("black");
        assertThat(response.layout()).isEqualTo("normal");
        assertThat(response.legalities()).containsEntry("standard", "legal");
        assertThat(response.priceUsd()).isEqualTo("0.65");
        assertThat(response.priceEur()).isEqualTo("2.02");
        assertThat(response.imageUrl()).isEqualTo("http://img");
        assertThat(response.imageLargeUrl()).isEqualTo("http://large");
        assertThat(response.artCropUrl()).isEqualTo("http://crop");
        assertThat(response.condition()).isEqualTo(MagicCardCondition.GOOD);
        assertThat(response.isFoil()).isFalse();
        assertThat(response.quantity()).isEqualTo(2);
        assertThat(response.notes()).isEqualTo("Nota");
        assertThat(response.dateAdded()).isEqualTo(LocalDateTime.of(2026, 1, 1, 12, 0));
    }

    @Test
    void toResponse_returnsNull_whenCardIsNull() {
        assertThat(mapper.toResponse(null)).isNull();
    }
}
