package com.wikicollection.domain.port.in;

import com.wikicollection.domain.model.Deck;
import com.wikicollection.domain.model.DeckStatus;
import com.wikicollection.domain.model.DeckStatusReport;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface DeckUseCase {

    Page<Deck> findAll(Pageable pageable);

    Page<Deck> findByName(String name, Pageable pageable);

    Deck findById(String id);

    Deck save(Deck deck);

    Deck update(String id, Deck updates);

    void delete(String id);

    Deck addCard(String deckId, String scryfallId, int quantity);

    Deck removeCard(String deckId, String scryfallId);

    DeckStatus getStatus(String deckId);

    DeckStatusReport getStatusReport(String deckId);
}
