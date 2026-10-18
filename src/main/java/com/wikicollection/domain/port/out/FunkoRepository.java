package com.wikicollection.domain.port.out;

import java.util.Optional;

import com.wikicollection.domain.model.Funko;
import com.wikicollection.domain.model.FunkoSearchCriteria;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface FunkoRepository {

    Page<Funko> search(FunkoSearchCriteria criteria, Pageable pageable);

    Optional<Funko> findById(String id);

    Optional<Funko> findByExternalId(String externalId);

    Funko save(Funko funko);

    void deleteById(String id);

    void deleteByOwnerId(String ownerId);

    void updateOwnerName(String ownerId, String ownerName);
}
