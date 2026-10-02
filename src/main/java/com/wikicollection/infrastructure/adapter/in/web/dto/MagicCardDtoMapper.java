package com.wikicollection.infrastructure.adapter.in.web.dto;

import com.wikicollection.domain.model.MagicCard;
import com.wikicollection.domain.model.MagicCardPrinting;

import org.springframework.stereotype.Component;

@Component
public class MagicCardDtoMapper {

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
                magicCard.getDateAdded(),
                UserOwnedResponse.from(magicCard.getUserOwned()));
    }

    public MagicCardPrintingResponse toPrintingResponse(MagicCardPrinting printing) {
        if (printing == null) {
            return null;
        }
        return new MagicCardPrintingResponse(
                printing.scryfallId(),
                printing.name(),
                printing.setCode(),
                printing.setName(),
                printing.collectorNumber(),
                printing.rarity(),
                printing.artist(),
                printing.releasedAt(),
                printing.lang(),
                printing.imageUrl(),
                printing.artCropUrl(),
                printing.finishes(),
                printing.fullArt(),
                printing.promoTypes(),
                printing.frameEffects(),
                printing.borderColor(),
                printing.priceUsd(),
                printing.priceEur());
    }
}
