package com.wikicollection.application.service;

import com.wikicollection.application.exception.FunkoConflictException;
import com.wikicollection.application.exception.FunkoNotFoundException;
import com.wikicollection.domain.model.CollectionType;
import com.wikicollection.domain.model.Funko;
import com.wikicollection.domain.model.FunkoSearchCriteria;
import com.wikicollection.domain.port.in.FunkoUseCase;
import com.wikicollection.domain.port.out.FunkoRepository;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class FunkoService implements FunkoUseCase {

    private final FunkoRepository funkoRepository;
    private final OwnershipValidator ownershipValidator;
    private final OwnerScopeResolver ownerScopeResolver;
    private final OwnerResolver ownerResolver;

    public FunkoService(FunkoRepository funkoRepository,
                        OwnershipValidator ownershipValidator,
                        OwnerScopeResolver ownerScopeResolver,
                        OwnerResolver ownerResolver) {
        this.funkoRepository = funkoRepository;
        this.ownershipValidator = ownershipValidator;
        this.ownerScopeResolver = ownerScopeResolver;
        this.ownerResolver = ownerResolver;
    }

    @Override
    @Cacheable(cacheNames = "funkoList", key = "T(java.util.Objects).hash(#criteria, #pageable, #owner, #viewerId)")
    public Page<Funko> search(FunkoSearchCriteria criteria, Pageable pageable, String owner, String viewerId) {
        OwnerScopeResolver.Scope scope = ownerScopeResolver.resolve(CollectionType.FUNKOS, owner, viewerId);
        return funkoRepository.search(
                new FunkoSearchCriteria(criteria.name(), criteria.status(), scope.ownerId(), scope.excludeOwnerIds()),
                pageable);
    }

    @Override
    @Cacheable(cacheNames = "funkoDetail", key = "#id")
    public Funko findById(String id) {
        return funkoRepository.findById(id)
                .orElseThrow(() -> new FunkoNotFoundException("Funko no encontrado con id: " + id));
    }

    @Override
    @Transactional
    @CacheEvict(cacheNames = "funkoList", allEntries = true)
    public Funko save(Funko funko, String ownerId) {
        funko.setOwnerId(ownerId);
        funko.setUserOwned(ownerResolver.resolveOwner(ownerId));
        checkExternalIdUnique(funko.getExternalId(), null);
        return saveOrConflict(funko);
    }

    @Override
    @Transactional
    @CacheEvict(cacheNames = {"funkoDetail", "funkoList"}, allEntries = true)
    public Funko update(String id, Funko updates, String userId) {
        Funko existing = findById(id);
        ownershipValidator.validateOwner(existing.getOwnerId(), userId);
        checkExternalIdUnique(updates.getExternalId(), id);
        copyUpdatableFields(existing, updates);
        return saveOrConflict(existing);
    }

    @Override
    @CacheEvict(cacheNames = {"funkoDetail", "funkoList"}, allEntries = true)
    public void delete(String id, String userId) {
        Funko existing = findById(id);
        ownershipValidator.validateOwner(existing.getOwnerId(), userId);
        funkoRepository.deleteById(id);
    }

    private Funko saveOrConflict(Funko funko) {
        try {
            return funkoRepository.save(funko);
        } catch (DuplicateKeyException e) {
            throw new FunkoConflictException("Ya existe un funko con externalId: " + funko.getExternalId());
        }
    }

    private void checkExternalIdUnique(String externalId, String currentId) {
        if (externalId == null || externalId.isBlank()) {
            return;
        }
        funkoRepository.findByExternalId(externalId)
                .filter(existing -> currentId == null || !existing.getId().equals(currentId))
                .ifPresent(existing -> {
                    throw new FunkoConflictException("Ya existe un funko con externalId: " + externalId);
                });
    }

    private void copyUpdatableFields(Funko target, Funko source) {
        target.setExternalId(source.getExternalId());
        target.setName(source.getName());
        target.setSeries(source.getSeries());
        target.setNumber(source.getNumber());
        target.setCategory(source.getCategory());
        target.setExclusive(source.getExclusive());
        target.setVaulted(source.getVaulted());
        target.setReleaseYear(source.getReleaseYear());
        target.setFranchise(source.getFranchise());
        target.setPopType(source.getPopType());
        target.setRarityScore(source.getRarityScore());
        target.setImageUrl(source.getImageUrl());
        target.setStatus(source.getStatus());
        target.setNotes(source.getNotes());
        target.setDateAdded(source.getDateAdded());
        target.setAcquisitionDate(source.getAcquisitionDate());
        target.setAcquisitionPrice(source.getAcquisitionPrice());
    }
}
