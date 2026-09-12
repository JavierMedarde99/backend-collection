package com.wikicollection.domain.port.out;

import java.util.Optional;

import com.wikicollection.domain.model.Deck;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface DeckRepository {

    Optional<Deck> findById(String id);

    Deck save(Deck deck);

    void deleteById(String id);

    Page<Deck> findAll(Pageable pageable);

    Page<Deck> findByName(String name, Pageable pageable);
}
