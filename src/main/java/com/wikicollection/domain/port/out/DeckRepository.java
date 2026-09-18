package com.wikicollection.domain.port.out;

import java.util.Optional;

import java.util.Collection;

import com.wikicollection.domain.model.Deck;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface DeckRepository {

    Optional<Deck> findById(String id);

    Deck save(Deck deck);

    void deleteById(String id);

    Page<Deck> findAll(Pageable pageable);

    Page<Deck> findByName(String name, Pageable pageable);

    Page<Deck> findByOwnerId(String ownerId, Pageable pageable);

    Page<Deck> findByOwnerIdNotIn(Collection<String> ownerIds, Pageable pageable);

    Page<Deck> findByNameAndOwnerId(String name, String ownerId, Pageable pageable);

    Page<Deck> findByNameAndOwnerIdNotIn(String name, Collection<String> ownerIds, Pageable pageable);

    void updateOwnerName(String ownerId, String ownerName);
}
