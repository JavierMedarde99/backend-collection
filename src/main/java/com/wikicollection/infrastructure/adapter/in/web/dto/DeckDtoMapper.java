package com.wikicollection.infrastructure.adapter.in.web.dto;

import java.util.List;

import com.wikicollection.domain.model.Deck;
import com.wikicollection.domain.model.DeckCard;
import com.wikicollection.domain.model.DeckStatus;
import com.wikicollection.domain.model.DeckStatusReport;

import org.springframework.stereotype.Component;

@Component
public class DeckDtoMapper {

    public Deck toDomain(DeckRequest request) {
        if (request == null) {
            return null;
        }
        return Deck.builder()
                .name(request.name())
                .description(request.description())
                .commander(request.commander())
                .commanderColors(request.commanderColors())
                .build();
    }

    public DeckResponse toResponse(Deck deck) {
        if (deck == null) {
            return null;
        }
        List<DeckCardResponse> cards = deck.getCards() == null ? List.of()
                : deck.getCards().stream().map(this::toCardResponse).toList();
        return new DeckResponse(
                deck.getId(),
                deck.getName(),
                deck.getDescription(),
                deck.getCommander(),
                deck.getCommanderColors(),
                cards,
                deck.getCreatedAt(),
                deck.getUpdatedAt());
    }

    public DeckStatusResponse toStatusResponse(DeckStatusReport report) {
        if (report == null) {
            return null;
        }
        String message = report.status() == DeckStatus.INVALID && report.reasons() != null
                ? String.join("; ", report.reasons())
                : null;
        return new DeckStatusResponse(report.status(), message);
    }

    private DeckCardResponse toCardResponse(DeckCard card) {
        return new DeckCardResponse(
                card.getCardName(),
                card.getQuantity(),
                card.getInCollection(),
                card.getIsProxy(),
                card.getManaCost(),
                card.getTypeLine(),
                card.getColorIdentity(),
                card.getImageUrl(),
                card.getScryfallId());
    }
}
