package com.wikicollection.application.service;

import com.wikicollection.application.exception.AmiiboConflictException;
import com.wikicollection.application.exception.AmiiboNotFoundException;
import com.wikicollection.domain.model.Amiibo;
import com.wikicollection.domain.model.AmiiboSearchCriteria;
import com.wikicollection.domain.model.CollectionType;
import com.wikicollection.domain.port.in.AmiiboUseCase;
import com.wikicollection.domain.port.out.AmiiboRepository;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AmiiboService implements AmiiboUseCase {

    private final AmiiboRepository amiiboRepository;
    private final OwnershipValidator ownershipValidator;
    private final OwnerScopeResolver ownerScopeResolver;
    private final OwnerResolver ownerResolver;

    public AmiiboService(AmiiboRepository amiiboRepository,
                         OwnershipValidator ownershipValidator,
                         OwnerScopeResolver ownerScopeResolver,
                         OwnerResolver ownerResolver) {
        this.amiiboRepository = amiiboRepository;
        this.ownershipValidator = ownershipValidator;
        this.ownerScopeResolver = ownerScopeResolver;
        this.ownerResolver = ownerResolver;
    }

    @Override
    @Cacheable(cacheNames = "amiiboList", key = "T(java.util.Objects).hash(#criteria, #pageable, #owner, #viewerId)")
    public Page<Amiibo> search(AmiiboSearchCriteria criteria, Pageable pageable, String owner, String viewerId) {
        OwnerScopeResolver.Scope scope = ownerScopeResolver.resolve(CollectionType.AMIIBOS, owner, viewerId);
        return amiiboRepository.search(
                new AmiiboSearchCriteria(criteria.name(), criteria.status(), scope.ownerId(), scope.excludeOwnerIds()),
                pageable);
    }

    @Override
    @Cacheable(cacheNames = "amiiboDetail", key = "#id")
    public Amiibo findById(String id) {
        return amiiboRepository.findById(id)
                .orElseThrow(() -> new AmiiboNotFoundException("Amiibo no encontrado con id: " + id));
    }

    @Override
    @Transactional
    @CacheEvict(cacheNames = "amiiboList", allEntries = true)
    public Amiibo save(Amiibo amiibo, String ownerId) {
        amiibo.setOwnerId(ownerId);
        amiibo.setUserOwned(ownerResolver.resolveOwner(ownerId));
        checkExternalIdUnique(amiibo.getExternalId(), null);
        return saveOrConflict(amiibo);
    }

    @Override
    @Transactional
    @CacheEvict(cacheNames = {"amiiboDetail", "amiiboList"}, allEntries = true)
    public Amiibo update(String id, Amiibo updates, String userId) {
        Amiibo existing = findById(id);
        ownershipValidator.validateOwner(existing.getOwnerId(), userId);
        checkExternalIdUnique(updates.getExternalId(), id);
        copyUpdatableFields(existing, updates);
        return saveOrConflict(existing);
    }

    @Override
    @CacheEvict(cacheNames = {"amiiboDetail", "amiiboList"}, allEntries = true)
    public void delete(String id, String userId) {
        Amiibo existing = findById(id);
        ownershipValidator.validateOwner(existing.getOwnerId(), userId);
        amiiboRepository.deleteById(id);
    }

    private Amiibo saveOrConflict(Amiibo amiibo) {
        try {
            return amiiboRepository.save(amiibo);
        } catch (DuplicateKeyException e) {
            throw new AmiiboConflictException("Ya existe un amiibo con externalId: " + amiibo.getExternalId());
        }
    }

    private void checkExternalIdUnique(String externalId, String currentId) {
        if (externalId == null || externalId.isBlank()) {
            return;
        }
        amiiboRepository.findByExternalId(externalId)
                .filter(existing -> currentId == null || !existing.getId().equals(currentId))
                .ifPresent(existing -> {
                    throw new AmiiboConflictException("Ya existe un amiibo con externalId: " + externalId);
                });
    }

    private void copyUpdatableFields(Amiibo target, Amiibo source) {
        target.setExternalId(source.getExternalId());
        target.setName(source.getName());
        target.setCharacter(source.getCharacter());
        target.setAmiiboSeries(source.getAmiiboSeries());
        target.setGameSeries(source.getGameSeries());
        target.setAmiiboType(source.getAmiiboType());
        target.setReleaseDates(source.getReleaseDates());
        target.setImageUrl(source.getImageUrl());
        target.setCompatibleGames(source.getCompatibleGames());
        target.setStatus(source.getStatus());
        target.setNotes(source.getNotes());
        target.setDateAdded(source.getDateAdded());
        target.setAcquisitionDate(source.getAcquisitionDate());
        target.setAcquisitionPrice(source.getAcquisitionPrice());
    }
}
