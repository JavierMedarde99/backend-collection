package com.wikicollection.domain.port.in;

import java.util.List;

import com.wikicollection.domain.model.Deck;
import com.wikicollection.domain.model.DeckStatus;

public interface DeckUseCase {

    List<Deck> findAll();

    List<Deck> findByName(String name);

    Deck findById(String id);

    Deck save(Deck deck);

    Deck update(String id, Deck updates);

    void delete(String id);

    Deck addCard(String deckId, String scryfallId, int quantity);

    Deck removeCard(String deckId, String scryfallId);

    DeckStatus getStatus(String deckId);
}
