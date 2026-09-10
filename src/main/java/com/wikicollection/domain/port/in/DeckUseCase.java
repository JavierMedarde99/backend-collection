package com.wikicollection.domain.port.in;

import com.wikicollection.domain.model.Deck;
import com.wikicollection.domain.model.DeckStatus;

public interface DeckUseCase {

    Deck findById(String id);

    Deck save(Deck deck);

    Deck update(String id, Deck updates);

    void delete(String id);

    Deck addCard(String deckId, String scryfallId, int quantity);

    Deck removeCard(String deckId, String scryfallId);

    DeckStatus getStatus(String deckId);
}
