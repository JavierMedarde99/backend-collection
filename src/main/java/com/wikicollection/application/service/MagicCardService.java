package com.wikicollection.application.service;

import com.wikicollection.application.exception.MagicCardNotFoundException;
import com.wikicollection.domain.model.MagicCard;
import com.wikicollection.domain.model.MagicCardPrinting;
import com.wikicollection.domain.model.MagicCardSearchCriteria;
import com.wikicollection.domain.port.in.MagicCardUseCase;
import com.wikicollection.domain.port.out.ExternalMagicCardCatalogClient;
import com.wikicollection.domain.port.out.MagicCardRepository;

import com.wikicollection.domain.model.CollectionType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientResponseException;

@Service
public class MagicCardService implements MagicCardUseCase {

    private final MagicCardRepository magicCardRepository;
    private final ExternalMagicCardCatalogClient catalogClient;
    private final OwnershipValidator ownershipValidator;

    private final OwnerScopeResolver ownerScopeResolver;

    private final OwnerResolver ownerResolver;

    public MagicCardService(MagicCardRepository magicCardRepository,
                            ExternalMagicCardCatalogClient catalogClient,
                            OwnershipValidator ownershipValidator,
                       OwnerScopeResolver ownerScopeResolver,
                       OwnerResolver ownerResolver) {
        this.magicCardRepository = magicCardRepository;
        this.catalogClient = catalogClient;
        this.ownershipValidator = ownershipValidator;
        this.ownerResolver = ownerResolver;
        this.ownerScopeResolver = ownerScopeResolver;
    }

    @Override
    public Page<MagicCard> search(MagicCardSearchCriteria criteria, Pageable pageable) {
        return magicCardRepository.search(criteria, pageable);
    }

    @Override
    @Cacheable(cacheNames = "magicList", key = "T(java.util.Objects).hash(#criteria, #pageable, #owner, #viewerId)")
    public Page<MagicCard> search(MagicCardSearchCriteria criteria, Pageable pageable, String owner, String viewerId) {
        OwnerScopeResolver.Scope scope = ownerScopeResolver.resolve(CollectionType.MAGIC, owner, viewerId);
        return magicCardRepository.search(new MagicCardSearchCriteria(criteria.name(), criteria.rarity(), criteria.color(), criteria.type(), scope.ownerId(), scope.excludeOwnerIds()), pageable);
    }

    @Override
    @Cacheable(cacheNames = "magicDetail", key = "#id")
    public MagicCard findById(String id) {
        return magicCardRepository.findById(id)
                .orElseThrow(() -> new MagicCardNotFoundException("Carta no encontrada con id: " + id));
    }

    @Override
    @CacheEvict(cacheNames = "magicList", allEntries = true)
    public MagicCard addFromScryfall(String scryfallId, int quantity, String ownerId) {
        if (quantity < 1) {
            throw new IllegalArgumentException("La cantidad mínima es 1");
        }
        MagicCard fetched;
        try {
            fetched = catalogClient.findById(scryfallId);
        } catch (RestClientResponseException e) {
            if (e.getStatusCode().value() == HttpStatus.NOT_FOUND.value()) {
                throw new MagicCardNotFoundException("Carta no encontrada en Scryfall con id: " + scryfallId);
            }
            throw e;
        }
        fetched.setQuantity(quantity);
        fetched.setOwnerId(ownerId);
        fetched.setUserOwned(ownerResolver.resolveOwner(ownerId));
        return magicCardRepository.save(fetched);
    }

    @Override
    public Page<MagicCardPrinting> printings(String scryfallId, int page) {
        String oracleId = resolveOracleId(scryfallId);
        if (oracleId == null) {
            // Scryfall no tiene oracle_id en todas las respuestas. Consultar "oracleid:"
            // sin valor devuelve 200 con 0 resultados, que el usuario leería como "esta
            // carta no tiene reimpresiones"; una página vacía explícita dice lo mismo pero
            // sin gastar una llamada ni fingir que se ha consultado.
            return new PageImpl<>(java.util.List.of(),
                    PageRequest.of(Math.max(page, 0), MagicCardPrinting.PAGE_SIZE), 0);
        }
        return catalogClient.findPrintings(oracleId, page);
    }

    /** El endpoint recibe un id de impresión, pero Scryfall solo pagina por oracle_id. */
    private String resolveOracleId(String scryfallId) {
        MagicCard card;
        try {
            card = catalogClient.findById(scryfallId);
        } catch (RestClientResponseException e) {
            if (e.getStatusCode().value() == HttpStatus.NOT_FOUND.value()) {
                throw new MagicCardNotFoundException("Carta no encontrada en Scryfall con id: " + scryfallId);
            }
            throw e;
        }
        if (card == null || card.getOracleId() == null || card.getOracleId().isBlank()) {
            return null;
        }
        return card.getOracleId();
    }

    @Override
    @CacheEvict(cacheNames = {"magicDetail", "magicList"}, allEntries = true)
    public void delete(String id, String userId) {
        MagicCard existing = findById(id);
        ownershipValidator.validateOwner(existing.getOwnerId(), userId);
        magicCardRepository.deleteById(id);
    }
}
