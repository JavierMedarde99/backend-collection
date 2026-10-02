package com.wikicollection.infrastructure.adapter.out.scryfall;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.wikicollection.domain.model.MagicCard;
import com.wikicollection.domain.model.MagicCardPrinting;
import com.wikicollection.domain.model.MagicCardSearchResult;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
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

    public MagicCardPrinting toPrinting(ScryfallCardResponse card) {
        if (card == null) {
            return null;
        }
        return new MagicCardPrinting(
                card.id(),
                card.name(),
                card.set(),
                card.setName(),
                card.collectorNumber(),
                card.rarity(),
                card.artist(),
                card.releasedAt(),
                card.lang(),
                card.imageUris() != null ? card.imageUris().normal() : null,
                card.imageUris() != null ? card.imageUris().artCrop() : null,
                card.finishes(),
                Boolean.TRUE.equals(card.fullArt()),
                card.promoTypes(),
                card.frameEffects(),
                card.borderColor(),
                card.prices() != null ? card.prices().usd() : null,
                card.prices() != null ? card.prices().eur() : null);
    }

    /**
     * Convierte una pagina de impresiones de Scryfall. El {@code page} es base 0 y el
     * tamano se fija al de Scryfall porque su API no permite pedir menos.
     */
    public Page<MagicCardPrinting> mapPrintings(ScryfallListResponse response, int page) {
        List<MagicCardPrinting> printings = response == null || response.data() == null
                ? List.of()
                : response.data().stream().map(this::toPrinting).toList();
        long total = response != null && response.totalCards() != null
                ? response.totalCards()
                : printings.size();
        return new PageImpl<>(printings, PageRequest.of(Math.max(page, 0), MagicCardPrinting.PAGE_SIZE), total);
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

    // Scryfall añade object, has_more, next_page y warnings: sin ignorar campos
    // desconocidos esta deserialización depende de la configuración global de Jackson.
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record ScryfallListResponse(
            @JsonProperty("data") List<ScryfallCardResponse> data,
            @JsonProperty("total_cards") Long totalCards) {
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
            @JsonProperty("image_uris") ScryfallImageUris imageUris,
            @JsonProperty("collector_number") String collectorNumber,
            @JsonProperty("lang") String lang,
            @JsonProperty("finishes") List<String> finishes,
            @JsonProperty("full_art") Boolean fullArt,
            @JsonProperty("promo_types") List<String> promoTypes,
            @JsonProperty("frame_effects") List<String> frameEffects,
            @JsonProperty("promo_prices") ScryfallPromoPrices promoPrices) {
    }

    /** Acabados especiales (foil, etched). No se exponen: el DTO solo lleva el precio simple. */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record ScryfallPromoPrices(
            @JsonProperty("usd") String usd,
            @JsonProperty("eur") String eur) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record ScryfallPrices(
            @JsonProperty("usd") String usd,
            @JsonProperty("eur") String eur) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record ScryfallImageUris(
            @JsonProperty("normal") String normal,
            @JsonProperty("large") String large,
            @JsonProperty("art_crop") String artCrop) {
    }
}
