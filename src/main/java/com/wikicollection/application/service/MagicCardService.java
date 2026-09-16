package com.wikicollection.application.service;

import com.wikicollection.application.exception.MagicCardNotFoundException;
import com.wikicollection.domain.model.MagicCard;
import com.wikicollection.domain.model.MagicCardSearchCriteria;
import com.wikicollection.domain.port.in.MagicCardUseCase;
import com.wikicollection.domain.port.out.ExternalMagicCardCatalogClient;
import com.wikicollection.domain.port.out.MagicCardRepository;

import com.wikicollection.domain.model.CollectionType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientResponseException;

@Service
public class MagicCardService implements MagicCardUseCase {

    private final MagicCardRepository magicCardRepository;
    private final ExternalMagicCardCatalogClient catalogClient;
    private final OwnershipValidator ownershipValidator;

    private final OwnerScopeResolver ownerScopeResolver;

    public MagicCardService(MagicCardRepository magicCardRepository,
                            ExternalMagicCardCatalogClient catalogClient,
                            OwnershipValidator ownershipValidator,
                       OwnerScopeResolver ownerScopeResolver) {
        this.magicCardRepository = magicCardRepository;
        this.catalogClient = catalogClient;
        this.ownershipValidator = ownershipValidator;
        this.ownerScopeResolver = ownerScopeResolver;
    }

    @Override
    public Page<MagicCard> search(MagicCardSearchCriteria criteria, Pageable pageable) {
        return magicCardRepository.search(criteria, pageable);
    }

    @Override
    public Page<MagicCard> search(MagicCardSearchCriteria criteria, Pageable pageable, String owner, String viewerId) {
        OwnerScopeResolver.Scope scope = ownerScopeResolver.resolve(CollectionType.MAGIC, owner, viewerId);
        return magicCardRepository.search(new MagicCardSearchCriteria(criteria.name(), criteria.rarity(), criteria.color(), criteria.type(), scope.ownerId(), scope.excludeOwnerIds()), pageable);
    }

    @Override
    public MagicCard findById(String id) {
        return magicCardRepository.findById(id)
                .orElseThrow(() -> new MagicCardNotFoundException("Carta no encontrada con id: " + id));
    }

    @Override
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
        return magicCardRepository.save(fetched);
    }

    @Override
    public void delete(String id, String userId) {
        MagicCard existing = findById(id);
        ownershipValidator.validateOwner(existing.getOwnerId(), userId);
        magicCardRepository.deleteById(id);
    }
}
