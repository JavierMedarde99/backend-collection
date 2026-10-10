package com.wikicollection.application.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.wikicollection.application.exception.DeckNotFoundException;
import com.wikicollection.application.exception.MagicCardNotFoundException;
import com.wikicollection.domain.model.CollectionType;
import com.wikicollection.domain.model.Deck;
import com.wikicollection.domain.model.DeckCard;
import com.wikicollection.domain.model.DeckStatus;
import com.wikicollection.domain.model.DeckStatusReport;
import com.wikicollection.domain.model.MagicCard;
import com.wikicollection.domain.port.in.DeckUseCase;
import com.wikicollection.domain.port.out.DeckRepository;
import com.wikicollection.domain.port.out.ExternalMagicCardCatalogClient;
import com.wikicollection.domain.port.out.MagicCardRepository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientResponseException;

@Service
public class DeckService implements DeckUseCase {

    private final DeckRepository deckRepository;
    private final ExternalMagicCardCatalogClient catalogClient;
    private final MagicCardRepository magicCardRepository;
    private final DeckValidator validator;
    private final OwnershipValidator ownershipValidator;
    private final OwnerScopeResolver ownerScopeResolver;
    private final DeckCardFactory cardFactory;
    private final DeckNameNormalizer normalizer;

    private final OwnerResolver ownerResolver;

    public DeckService(DeckRepository deckRepository,
                       ExternalMagicCardCatalogClient catalogClient,
                       MagicCardRepository magicCardRepository,
                       DeckValidator validator,
                       OwnershipValidator ownershipValidator,
                       OwnerScopeResolver ownerScopeResolver,
                       DeckCardFactory cardFactory,
                       DeckNameNormalizer normalizer,
                       OwnerResolver ownerResolver) {
        this.deckRepository = deckRepository;
        this.catalogClient = catalogClient;
        this.magicCardRepository = magicCardRepository;
        this.validator = validator;
        this.ownershipValidator = ownershipValidator;
        this.ownerResolver = ownerResolver;
        this.ownerScopeResolver = ownerScopeResolver;
        this.cardFactory = cardFactory;
        this.normalizer = normalizer;
    }

    @Override
    public Page<Deck> findAll(Pageable pageable) {
        return deckRepository.findAll(pageable);
    }

    @Override
    @Cacheable(cacheNames = "deckList", key = "T(java.util.Objects).hash(#pageable, #owner, #viewerId)")
    public Page<Deck> findAll(Pageable pageable, String owner, String viewerId) {
        OwnerScopeResolver.Scope scope = ownerScopeResolver.resolve(CollectionType.DECKS, owner, viewerId);
        Page<Deck> decks = scope.ownerId() != null
                ? deckRepository.findByOwnerId(scope.ownerId(), pageable)
                : deckRepository.findByOwnerIdNotIn(scope.excludeOwnerIds(), pageable);
        return markCollectionStatus(decks);
    }

    @Override
    public Page<Deck> findByName(String name, Pageable pageable) {
        return deckRepository.findByName(name, pageable);
    }

    @Override
    @Cacheable(cacheNames = "deckList", key = "T(java.util.Objects).hash(#name, #pageable, #owner, #viewerId)")
    public Page<Deck> findByName(String name, Pageable pageable, String owner, String viewerId) {
        OwnerScopeResolver.Scope scope = ownerScopeResolver.resolve(CollectionType.DECKS, owner, viewerId);
        Page<Deck> decks = scope.ownerId() != null
                ? deckRepository.findByNameAndOwnerId(name, scope.ownerId(), pageable)
                : deckRepository.findByNameAndOwnerIdNotIn(name, scope.excludeOwnerIds(), pageable);
        return markCollectionStatus(decks);
    }

    @Override
    @Cacheable(cacheNames = "deckDetail", key = "#id")
    public Deck findById(String id) {
        return markCollectionStatus(loadDeck(id), new HashMap<>());
    }

    private Deck loadDeck(String id) {
        return deckRepository.findById(id)
                .orElseThrow(() -> new DeckNotFoundException("Mazo no encontrado con id: " + id));
    }

    @Override
    @CacheEvict(cacheNames = "deckList", allEntries = true)
    public Deck save(Deck deck, String ownerId) {
        deck.setOwnerId(ownerId);
        deck.setUserOwned(ownerResolver.resolveOwner(ownerId));
        requireName(deck);
        deck.setCreatedAt(deck.getCreatedAt() != null ? deck.getCreatedAt() : LocalDateTime.now());
        deck.setUpdatedAt(LocalDateTime.now());
        return deckRepository.save(deck);
    }

    @Override
    @CacheEvict(cacheNames = {"deckDetail", "deckList"}, allEntries = true)
    public Deck update(String id, Deck updates, String userId) {
        Deck existing = findById(id);
        ownershipValidator.validateOwner(existing.getOwnerId(), userId);
        existing.setName(updates.getName());
        existing.setDescription(updates.getDescription());
        existing.setCommander(updates.getCommander());
        existing.setCommanderColors(updates.getCommanderColors());
        existing.setUpdatedAt(LocalDateTime.now());
        requireName(existing);
        return deckRepository.save(existing);
    }

    @Override
    @CacheEvict(cacheNames = {"deckDetail", "deckList"}, allEntries = true)
    public void delete(String id, String userId) {
        Deck existing = findById(id);
        ownershipValidator.validateOwner(existing.getOwnerId(), userId);
        deckRepository.deleteById(id);
    }

    @Override
    @CacheEvict(cacheNames = {"deckDetail", "deckList"}, allEntries = true)
    public Deck addCard(String deckId, String scryfallId, int quantity, String userId) {
        if (quantity < 1) {
            throw new IllegalArgumentException("La cantidad mínima es 1");
        }
        Deck deck = loadDeck(deckId);
        ownershipValidator.validateOwner(deck.getOwnerId(), userId);
        MagicCard fetched = fetchFromCatalog(scryfallId);
        List<DeckCard> cards = deck.getCards() == null ? new ArrayList<>() : new ArrayList<>(deck.getCards());
        cards.stream()
                .filter(card -> scryfallId.equals(card.getScryfallId()))
                .findFirst()
                .ifPresentOrElse(
                        card -> card.setQuantity(card.getQuantity() + quantity),
                        () -> cards.add(cardFactory.fromMagicCard(fetched, quantity, false)));
        deck.setCards(cards);
        deck.setUpdatedAt(LocalDateTime.now());
        return deckRepository.save(markCollectionStatus(deck, new HashMap<>()));
    }

    @Override
    @CacheEvict(cacheNames = {"deckDetail", "deckList"}, allEntries = true)
    public Deck removeCard(String deckId, String scryfallId, String userId) {
        Deck deck = findById(deckId);
        ownershipValidator.validateOwner(deck.getOwnerId(), userId);
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

    // ------------------------------------------------------ estado "en colección"

    /**
     * El estado "en colección" de una carta de mazo no es un dato del mazo: se deriva de la
     * colección de Magic de su dueño. La importación (#353) y {@link #addCard} lo guardan como
     * pista, pero la colección cambia por su cuenta (añadir una carta proxy desde el mazo o
     * borrarla), así que al servir un mazo hay que recalcularlo contra el inventario actual.
     * Sin esto, una carta añadida a la colección seguiría marcada como proxy al recargar.
     */
    private Page<Deck> markCollectionStatus(Page<Deck> decks) {
        Map<String, Set<String>> ownedByOwner = new HashMap<>();
        decks.getContent().forEach(deck -> markCollectionStatus(deck, ownedByOwner));
        return decks;
    }

    private Deck markCollectionStatus(Deck deck, Map<String, Set<String>> ownedByOwner) {
        if (deck == null) {
            return deck;
        }
        List<DeckCard> cards = deck.getCards();
        boolean hasCommander = deck.getCommander() != null && !deck.getCommander().isBlank();
        if ((cards == null || cards.isEmpty()) && !hasCommander) {
            return deck;
        }
        Set<String> owned = ownedNames(deck.getOwnerId(), ownedByOwner);
        if (cards != null) {
            for (DeckCard card : cards) {
                boolean inCollection = owned.contains(normalizer.normalize(card.getCardName()));
                card.setInCollection(inCollection);
                card.setIsProxy(!inCollection);
            }
        }
        if (hasCommander) {
            boolean commanderInCollection = owned.contains(normalizer.normalize(deck.getCommander()));
            deck.setCommanderInCollection(commanderInCollection);
            deck.setCommanderIsProxy(!commanderInCollection);
        }
        return deck;
    }

    /**
     * Nombres de la colección del dueño, normalizados y leídos una sola vez por dueño y
     * llamada: una página de mazos suele compartir dueño y no debe repetir la consulta.
     */
    private Set<String> ownedNames(String ownerId, Map<String, Set<String>> ownedByOwner) {
        if (ownerId == null || ownerId.isBlank()) {
            return Set.of();
        }
        return ownedByOwner.computeIfAbsent(ownerId,
                id -> normalizer.normalizeAll(magicCardRepository.findNamesByOwnerId(id)));
    }
}
