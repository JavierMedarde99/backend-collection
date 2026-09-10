package com.wikicollection.domain.port.out;

import java.util.List;
import java.util.Optional;

import com.wikicollection.domain.model.Deck;

public interface DeckRepository {

    Optional<Deck> findById(String id);

    Deck save(Deck deck);

    void deleteById(String id);

    List<Deck> findAll();

    List<Deck> findByName(String name);
}
