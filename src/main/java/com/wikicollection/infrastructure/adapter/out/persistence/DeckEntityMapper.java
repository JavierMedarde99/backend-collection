package com.wikicollection.infrastructure.adapter.out.persistence;

import java.util.List;

import com.wikicollection.domain.model.Deck;
import com.wikicollection.domain.model.DeckCard;

import org.springframework.stereotype.Component;

@Component
public class DeckEntityMapper {

    public DeckEntity toEntity(Deck deck) {
        if (deck == null) {
            return null;
        }
        return DeckEntity.builder()
                .id(deck.getId())
                .name(deck.getName())
                .description(deck.getDescription())
                .commander(deck.getCommander())
                .commanderColors(deck.getCommanderColors())
                .cards(toEntityCards(deck.getCards()))
                .createdAt(deck.getCreatedAt())
                .updatedAt(deck.getUpdatedAt())
                .build();
    }

    public Deck toDomain(DeckEntity entity) {
        if (entity == null) {
            return null;
        }
        return Deck.builder()
                .id(entity.getId())
                .name(entity.getName())
                .description(entity.getDescription())
                .commander(entity.getCommander())
                .commanderColors(entity.getCommanderColors())
                .cards(toDomainCards(entity.getCards()))
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }

    private List<DeckCardEntity> toEntityCards(List<DeckCard> cards) {
        if (cards == null) {
            return null;
        }
        return cards.stream().map(card -> DeckCardEntity.builder()
                .cardName(card.getCardName())
                .quantity(card.getQuantity())
                .inCollection(card.getInCollection())
                .isProxy(card.getIsProxy())
                .manaCost(card.getManaCost())
                .typeLine(card.getTypeLine())
                .colorIdentity(card.getColorIdentity())
                .imageUrl(card.getImageUrl())
                .scryfallId(card.getScryfallId())
                .build()).toList();
    }

    private List<DeckCard> toDomainCards(List<DeckCardEntity> entities) {
        if (entities == null) {
            return null;
        }
        return entities.stream().map(entity -> DeckCard.builder()
                .cardName(entity.getCardName())
                .quantity(entity.getQuantity())
                .inCollection(entity.getInCollection())
                .isProxy(entity.getIsProxy())
                .manaCost(entity.getManaCost())
                .typeLine(entity.getTypeLine())
                .colorIdentity(entity.getColorIdentity())
                .imageUrl(entity.getImageUrl())
                .scryfallId(entity.getScryfallId())
                .build()).toList();
    }
}
