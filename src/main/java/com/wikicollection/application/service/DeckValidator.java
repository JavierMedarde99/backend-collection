package com.wikicollection.application.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import com.wikicollection.domain.model.Deck;
import com.wikicollection.domain.model.DeckCard;
import com.wikicollection.domain.model.DeckStatus;

import org.springframework.stereotype.Component;

/**
 * Reglas del formato Commander: mazo de 100 cartas (comandante + 99),
 * singleton (salvo tierras básicas), identidad de color del comandante
 * y lista de baneadas (no exhaustiva, revisable).
 */
@Component
public class DeckValidator {

    public static final int COMMANDER_DECK_SIZE = 100;

    private static final Set<String> BANNED_CARDS = Set.of(
            "black lotus", "ancestral recall", "time walk", "timetwister",
            "mox pearl", "mox sapphire", "mox jet", "mox ruby", "mox emerald",
            "channel", "fastbond", "balance", "gifts ungiven",
            "emrakul, the aeons torn", "griselbrand", "primeval titan",
            "sylvan primordial", "prophet of kruphix", "leovold, emissary of trest",
            "hullbreacher", "iona, shield of emeria", "paradox engine",
            "flash", "lutri, the spellchaser", "golos, tireless pilgrim",
            "coalition victory", "biorhythm", "braids, cabal minion");

    public List<String> validate(Deck deck) {
        List<String> violations = new ArrayList<>();
        if (deck == null) {
            return List.of("Mazo nulo");
        }
        List<DeckCard> cards = deck.getCards() == null ? List.of() : deck.getCards();

        int total = 0;
        Map<String, DeckCard> seen = new HashMap<>();
        for (DeckCard card : cards) {
            if (card.getQuantity() == null || card.getQuantity() < 1) {
                violations.add("Cantidad inválida para: " + card.getCardName());
                continue;
            }
            total += card.getQuantity();
            String key = card.getScryfallId() != null
                    ? "id:" + card.getScryfallId()
                    : "name:" + String.valueOf(card.getCardName()).toLowerCase(Locale.ROOT);
            DeckCard previous = seen.putIfAbsent(key, card);
            if ((previous != null || card.getQuantity() > 1) && !isBasicLand(card)) {
                violations.add("La carta no respeta singleton: " + card.getCardName());
            }
            if (card.getCardName() != null
                    && BANNED_CARDS.contains(card.getCardName().toLowerCase(Locale.ROOT))) {
                violations.add("La carta está baneada en Commander: " + card.getCardName());
            }
            if (deck.getCommanderColors() != null && !deck.getCommanderColors().isEmpty()
                    && card.getColorIdentity() != null) {
                for (String color : card.getColorIdentity()) {
                    if (!deck.getCommanderColors().contains(color)) {
                        violations.add("La carta está fuera de la identidad de color del comandante: "
                                + card.getCardName());
                        break;
                    }
                }
            }
        }
        if (total > COMMANDER_DECK_SIZE - 1) {
            violations.add("El mazo supera las 100 cartas (comandante incluido)");
        }
        return violations;
    }

    public DeckStatus evaluate(Deck deck) {
        if (!validate(deck).isEmpty()) {
            return DeckStatus.INVALID;
        }
        List<DeckCard> cards = deck.getCards() == null ? List.of() : deck.getCards();
        int total = cards.stream()
                .mapToInt(card -> card.getQuantity() == null ? 0 : card.getQuantity())
                .sum();
        if (deck.getCommander() != null && !deck.getCommander().isBlank()
                && total == COMMANDER_DECK_SIZE - 1) {
            return DeckStatus.COMPLETE;
        }
        return DeckStatus.DRAFT;
    }

    private boolean isBasicLand(DeckCard card) {
        return card.getTypeLine() != null
                && card.getTypeLine().toLowerCase(Locale.ROOT).contains("basic land");
    }
}
