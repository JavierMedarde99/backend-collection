package com.wikicollection.infrastructure.adapter.out.persistence;

import com.wikicollection.domain.model.MagicCard;

import org.springframework.stereotype.Component;

@Component
public class MagicCardEntityMapper {

    public MagicCardEntity toEntity(MagicCard magicCard) {
        if (magicCard == null) {
            return null;
        }
        return MagicCardEntity.builder()
                .id(magicCard.getId())
                .scryfallId(magicCard.getScryfallId())
                .oracleId(magicCard.getOracleId())
                .name(magicCard.getName())
                .language(magicCard.getLanguage())
                .releaseDate(magicCard.getReleaseDate())
                .manaCost(magicCard.getManaCost())
                .convertedManaCost(magicCard.getConvertedManaCost())
                .type(magicCard.getType())
                .text(magicCard.getText())
                .power(magicCard.getPower())
                .toughness(magicCard.getToughness())
                .loyalty(magicCard.getLoyalty())
                .colors(magicCard.getColors())
                .colorIdentity(magicCard.getColorIdentity())
                .keywords(magicCard.getKeywords())
                .rarity(magicCard.getRarity())
                .setCode(magicCard.getSetCode())
                .setName(magicCard.getSetName())
                .artist(magicCard.getArtist())
                .frame(magicCard.getFrame())
                .borderColor(magicCard.getBorderColor())
                .layout(magicCard.getLayout())
                .legalities(magicCard.getLegalities())
                .priceUsd(magicCard.getPriceUsd())
                .priceEur(magicCard.getPriceEur())
                .imageUrl(magicCard.getImageUrl())
                .imageLargeUrl(magicCard.getImageLargeUrl())
                .artCropUrl(magicCard.getArtCropUrl())
                .condition(magicCard.getCondition())
                .isFoil(magicCard.getIsFoil())
                .quantity(magicCard.getQuantity())
                .notes(magicCard.getNotes())
                .dateAdded(magicCard.getDateAdded())
                .build();
    }

    public MagicCard toDomain(MagicCardEntity entity) {
        if (entity == null) {
            return null;
        }
        return MagicCard.builder()
                .id(entity.getId())
                .scryfallId(entity.getScryfallId())
                .oracleId(entity.getOracleId())
                .name(entity.getName())
                .language(entity.getLanguage())
                .releaseDate(entity.getReleaseDate())
                .manaCost(entity.getManaCost())
                .convertedManaCost(entity.getConvertedManaCost())
                .type(entity.getType())
                .text(entity.getText())
                .power(entity.getPower())
                .toughness(entity.getToughness())
                .loyalty(entity.getLoyalty())
                .colors(entity.getColors())
                .colorIdentity(entity.getColorIdentity())
                .keywords(entity.getKeywords())
                .rarity(entity.getRarity())
                .setCode(entity.getSetCode())
                .setName(entity.getSetName())
                .artist(entity.getArtist())
                .frame(entity.getFrame())
                .borderColor(entity.getBorderColor())
                .layout(entity.getLayout())
                .legalities(entity.getLegalities())
                .priceUsd(entity.getPriceUsd())
                .priceEur(entity.getPriceEur())
                .imageUrl(entity.getImageUrl())
                .imageLargeUrl(entity.getImageLargeUrl())
                .artCropUrl(entity.getArtCropUrl())
                .condition(entity.getCondition())
                .isFoil(entity.getIsFoil())
                .quantity(entity.getQuantity())
                .notes(entity.getNotes())
                .dateAdded(entity.getDateAdded())
                .build();
    }
}
