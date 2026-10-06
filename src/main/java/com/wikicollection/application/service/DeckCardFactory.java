package com.wikicollection.application.service;

import com.wikicollection.domain.model.DeckCard;
import com.wikicollection.domain.model.MagicCard;
import com.wikicollection.domain.model.MagicCardSearchResult;

import org.springframework.stereotype.Component;

/**
 * Mapeo de una carta a la entrada que guarda un mazo.
 *
 * <p>Existe para que la forma de una carta en el mazo la escriba un solo sitio: la usan tanto
 * {@link DeckService#addCard} como la importación de mazos (#353), que parte del resultado de
 * buscar un nombre en Scryfall en lugar de de una carta ya guardada.
 */
@Component
public class DeckCardFactory {

    /** Una carta que el dueño no tiene se marca como proxy: se puede jugar, pero no está en su colección. */
    public DeckCard fromMagicCard(MagicCard card, int quantity, boolean inCollection) {
        return DeckCard.builder()
                .cardName(card.getName())
                .quantity(quantity)
                .inCollection(inCollection)
                .isProxy(!inCollection)
                .manaCost(card.getManaCost())
                .typeLine(card.getType())
                .colorIdentity(card.getColorIdentity())
                .imageUrl(card.getImageUrl())
                .scryfallId(card.getScryfallId())
                .build();
    }

    /** Igual que {@link #fromMagicCard}, pero desde una carta que aún no está en la colección. */
    public DeckCard fromSearchResult(MagicCardSearchResult card, int quantity, boolean inCollection) {
        return DeckCard.builder()
                .cardName(card.name())
                .quantity(quantity)
                .inCollection(inCollection)
                .isProxy(!inCollection)
                .manaCost(card.manaCost())
                .typeLine(card.type())
                .colorIdentity(card.colorIdentity())
                .imageUrl(card.imageUrl())
                .scryfallId(card.scryfallId())
                .build();
    }
}