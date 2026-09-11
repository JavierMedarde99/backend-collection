package com.wikicollection.infrastructure.adapter.out.scryfall;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.wikicollection.domain.model.MagicCard;
import com.wikicollection.domain.model.MagicCardSearchResult;

import org.springframework.stereotype.Component;

@Component
public class MagicCardMapper {

    public List<MagicCardSearchResult> mapResponse(ScryfallListResponse response) {
        if (response == null || response.data() == null) {
            return List.of();
        }
        return response.data().stream()
                .map(this::toSearchResult)
                .toList();
    }

    public MagicCard map(ScryfallCardResponse card) {
        if (card == null) {
            return null;
        }
        return MagicCard.builder()
                .scryfallId(card.id())
                .oracleId(card.oracleId())
                .name(card.name())
                .releaseDate(card.releasedAt())
                .manaCost(card.manaCost())
                .convertedManaCost(card.cmc())
                .type(card.typeLine())
                .text(card.oracleText())
                .power(card.power())
                .toughness(card.toughness())
                .loyalty(card.loyalty())
                .colors(card.colors())
                .colorIdentity(card.colorIdentity())
                .keywords(card.keywords())
                .rarity(card.rarity())
                .setCode(card.set())
                .setName(card.setName())
                .artist(card.artist())
                .frame(card.frame())
                .borderColor(card.borderColor())
                .layout(card.layout())
                .legalities(card.legalities())
                .priceUsd(card.prices() != null ? card.prices().usd() : null)
                .priceEur(card.prices() != null ? card.prices().eur() : null)
                .imageUrl(card.imageUris() != null ? card.imageUris().normal() : null)
                .imageLargeUrl(card.imageUris() != null ? card.imageUris().large() : null)
                .artCropUrl(card.imageUris() != null ? card.imageUris().artCrop() : null)
                .build();
    }

    private MagicCardSearchResult toSearchResult(ScryfallCardResponse card) {
        return new MagicCardSearchResult(
                card.id(),
                card.name(),
                card.manaCost(),
                card.typeLine(),
                card.rarity(),
                card.set(),
                card.setName(),
                card.imageUris() != null ? card.imageUris().normal() : null,
                card.prices() != null ? card.prices().usd() : null,
                card.colors(),
                card.colorIdentity(),
                card.oracleText());
    }

    public record ScryfallListResponse(
            @JsonProperty("data") List<ScryfallCardResponse> data) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record ScryfallCardResponse(
            @JsonProperty("id") String id,
            @JsonProperty("oracle_id") String oracleId,
            @JsonProperty("name") String name,
            @JsonProperty("released_at") String releasedAt,
            @JsonProperty("mana_cost") String manaCost,
            @JsonProperty("cmc") Double cmc,
            @JsonProperty("type_line") String typeLine,
            @JsonProperty("oracle_text") String oracleText,
            @JsonProperty("power") String power,
            @JsonProperty("toughness") String toughness,
            @JsonProperty("loyalty") String loyalty,
            @JsonProperty("colors") List<String> colors,
            @JsonProperty("color_identity") List<String> colorIdentity,
            @JsonProperty("keywords") List<String> keywords,
            @JsonProperty("rarity") String rarity,
            @JsonProperty("set") String set,
            @JsonProperty("set_name") String setName,
            @JsonProperty("artist") String artist,
            @JsonProperty("frame") String frame,
            @JsonProperty("border_color") String borderColor,
            @JsonProperty("layout") String layout,
            @JsonProperty("legalities") java.util.Map<String, String> legalities,
            @JsonProperty("prices") ScryfallPrices prices,
            @JsonProperty("image_uris") ScryfallImageUris imageUris) {
    }

    public record ScryfallPrices(
            @JsonProperty("usd") String usd,
            @JsonProperty("eur") String eur) {
    }

    public record ScryfallImageUris(
            @JsonProperty("normal") String normal,
            @JsonProperty("large") String large,
            @JsonProperty("art_crop") String artCrop) {
    }
}
