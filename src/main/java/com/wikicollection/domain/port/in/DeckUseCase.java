package com.wikicollection.domain.port.in;

import com.wikicollection.domain.model.Deck;
import com.wikicollection.domain.model.DeckStatus;
import com.wikicollection.domain.model.DeckStatusReport;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface DeckUseCase {

    Page<Deck> findAll(Pageable pageable);

    Page<Deck> findAll(Pageable pageable, String owner, String viewerId);

    Page<Deck> findByName(String name, Pageable pageable);

    Page<Deck> findByName(String name, Pageable pageable, String owner, String viewerId);

    Deck findById(String id);

    Deck save(Deck deck, String ownerId);

    Deck update(String id, Deck updates, String userId);

    void delete(String id, String userId);

    Deck addCard(String deckId, String scryfallId, int quantity, String userId);

    Deck removeCard(String deckId, String scryfallId, String userId);

    DeckStatus getStatus(String deckId);

    DeckStatusReport getStatusReport(String deckId);
}
