package com.wikicollection.application.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import com.wikicollection.application.exception.DeckNotFoundException;
import com.wikicollection.application.exception.MagicCardNotFoundException;
import com.wikicollection.domain.model.Deck;
import com.wikicollection.domain.model.DeckCard;
import com.wikicollection.domain.model.DeckStatus;
import com.wikicollection.domain.model.DeckStatusReport;
import com.wikicollection.domain.model.MagicCard;
import com.wikicollection.domain.model.MagicCardSearchCriteria;
import com.wikicollection.domain.port.in.DeckUseCase;
import com.wikicollection.domain.port.out.DeckRepository;
import com.wikicollection.domain.port.out.ExternalMagicCardCatalogClient;
import com.wikicollection.domain.port.out.MagicCardRepository;

import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientResponseException;

@Service
public class DeckService implements DeckUseCase {

    private final DeckRepository deckRepository;
    private final ExternalMagicCardCatalogClient catalogClient;
    private final MagicCardRepository magicCardRepository;
    private final DeckValidator validator;

    public DeckService(DeckRepository deckRepository,
                       ExternalMagicCardCatalogClient catalogClient,
                       MagicCardRepository magicCardRepository,
                       DeckValidator validator) {
        this.deckRepository = deckRepository;
        this.catalogClient = catalogClient;
        this.magicCardRepository = magicCardRepository;
        this.validator = validator;
    }

    @Override
    public List<Deck> findAll() {
        return deckRepository.findAll();
    }

    @Override
    public List<Deck> findByName(String name) {
        return deckRepository.findByName(name);
    }

    @Override
    public Deck findById(String id) {
        return deckRepository.findById(id)
                .orElseThrow(() -> new DeckNotFoundException("Mazo no encontrado con id: " + id));
    }

    @Override
    public Deck save(Deck deck) {
        requireName(deck);
        deck.setCreatedAt(deck.getCreatedAt() != null ? deck.getCreatedAt() : LocalDateTime.now());
        deck.setUpdatedAt(LocalDateTime.now());
        return deckRepository.save(deck);
    }

    @Override
    public Deck update(String id, Deck updates) {
        Deck existing = findById(id);
        existing.setName(updates.getName());
        existing.setDescription(updates.getDescription());
        existing.setCommander(updates.getCommander());
        existing.setCommanderColors(updates.getCommanderColors());
        existing.setUpdatedAt(LocalDateTime.now());
        requireName(existing);
        return deckRepository.save(existing);
    }

    @Override
    public void delete(String id) {
        findById(id);
        deckRepository.deleteById(id);
    }

    @Override
    public Deck addCard(String deckId, String scryfallId, int quantity) {
        if (quantity < 1) {
            throw new IllegalArgumentException("La cantidad mínima es 1");
        }
        Deck deck = findById(deckId);
        MagicCard fetched = fetchFromCatalog(scryfallId);
        boolean owned = !magicCardRepository
                .search(new MagicCardSearchCriteria(fetched.getName()), PageRequest.of(0, 1))
                .isEmpty();
        List<DeckCard> cards = deck.getCards() == null ? new ArrayList<>() : new ArrayList<>(deck.getCards());
        cards.stream()
                .filter(card -> scryfallId.equals(card.getScryfallId()))
                .findFirst()
                .ifPresentOrElse(
                        card -> card.setQuantity(card.getQuantity() + quantity),
                        () -> cards.add(DeckCard.builder()
                                .cardName(fetched.getName())
                                .quantity(quantity)
                                .inCollection(owned)
                                .isProxy(!owned)
                                .manaCost(fetched.getManaCost())
                                .typeLine(fetched.getType())
                                .colorIdentity(fetched.getColorIdentity())
                                .imageUrl(fetched.getImageUrl())
                                .scryfallId(fetched.getScryfallId())
                                .build()));
        deck.setCards(cards);
        deck.setUpdatedAt(LocalDateTime.now());
        return deckRepository.save(deck);
    }

    @Override
    public Deck removeCard(String deckId, String scryfallId) {
        Deck deck = findById(deckId);
        List<DeckCard> cards = deck.getCards() == null ? new ArrayList<>() : new ArrayList<>(deck.getCards());
        boolean removed = cards.removeIf(card -> scryfallId.equals(card.getScryfallId()));
        if (!removed) {
            throw new IllegalArgumentException("La carta no está en el mazo: " + scryfallId);
        }
        deck.setCards(cards);
        deck.setUpdatedAt(LocalDateTime.now());
        return deckRepository.save(deck);
    }

    @Override
    public DeckStatus getStatus(String deckId) {
        return validator.evaluate(findById(deckId));
    }

    @Override
    public DeckStatusReport getStatusReport(String deckId) {
        Deck deck = findById(deckId);
        return new DeckStatusReport(validator.evaluate(deck), validator.validate(deck));
    }

    private MagicCard fetchFromCatalog(String scryfallId) {
        try {
            return catalogClient.findById(scryfallId);
        } catch (RestClientResponseException e) {
            if (e.getStatusCode().value() == HttpStatus.NOT_FOUND.value()) {
                throw new MagicCardNotFoundException("Carta no encontrada en Scryfall con id: " + scryfallId);
            }
            throw e;
        }
    }

    private void requireName(Deck deck) {
        if (deck.getName() == null || deck.getName().isBlank()) {
            throw new IllegalArgumentException("El nombre del mazo es obligatorio");
        }
    }
}
