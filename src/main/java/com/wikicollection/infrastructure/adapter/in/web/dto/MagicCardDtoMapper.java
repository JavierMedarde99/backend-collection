package com.wikicollection.infrastructure.adapter.in.web.dto;

import com.wikicollection.domain.model.MagicCard;

import org.springframework.stereotype.Component;

@Component
public class MagicCardDtoMapper {

    public MagicCard toDomain(MagicCardRequest request) {
        if (request == null) {
            return null;
        }
        return MagicCard.builder()
                .name(request.name())
                .language(request.language())
                .releaseDate(request.releaseDate())
                .manaCost(request.manaCost())
                .convertedManaCost(request.convertedManaCost())
                .type(request.type())
                .text(request.text())
                .power(request.power())
                .toughness(request.toughness())
                .loyalty(request.loyalty())
                .colors(request.colors())
                .colorIdentity(request.colorIdentity())
                .keywords(request.keywords())
                .rarity(request.rarity())
                .setCode(request.setCode())
                .setName(request.setName())
                .artist(request.artist())
                .frame(request.frame())
                .borderColor(request.borderColor())
                .layout(request.layout())
                .legalities(request.legalities())
                .priceUsd(request.priceUsd())
                .priceEur(request.priceEur())
                .imageUrl(request.imageUrl())
                .imageLargeUrl(request.imageLargeUrl())
                .artCropUrl(request.artCropUrl())
                .condition(request.condition())
                .isFoil(request.isFoil())
                .quantity(request.quantity())
                .notes(request.notes())
                .build();
    }

    public MagicCardResponse toResponse(MagicCard magicCard) {
        if (magicCard == null) {
            return null;
        }
        return new MagicCardResponse(
                magicCard.getId(),
                magicCard.getScryfallId(),
                magicCard.getOracleId(),
                magicCard.getName(),
                magicCard.getLanguage(),
                magicCard.getReleaseDate(),
                magicCard.getManaCost(),
                magicCard.getConvertedManaCost(),
                magicCard.getType(),
                magicCard.getText(),
                magicCard.getPower(),
                magicCard.getToughness(),
                magicCard.getLoyalty(),
                magicCard.getColors(),
                magicCard.getColorIdentity(),
                magicCard.getKeywords(),
                magicCard.getRarity(),
                magicCard.getSetCode(),
                magicCard.getSetName(),
                magicCard.getArtist(),
                magicCard.getFrame(),
                magicCard.getBorderColor(),
                magicCard.getLayout(),
                magicCard.getLegalities(),
                magicCard.getPriceUsd(),
                magicCard.getPriceEur(),
                magicCard.getImageUrl(),
                magicCard.getImageLargeUrl(),
                magicCard.getArtCropUrl(),
                magicCard.getCondition(),
                magicCard.getIsFoil(),
                magicCard.getQuantity(),
                magicCard.getNotes(),
                magicCard.getDateAdded());
    }
}
