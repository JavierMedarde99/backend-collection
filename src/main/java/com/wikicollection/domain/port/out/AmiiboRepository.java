package com.wikicollection.domain.port.out;

import java.util.Optional;

import com.wikicollection.domain.model.Amiibo;
import com.wikicollection.domain.model.AmiiboSearchCriteria;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface AmiiboRepository {

    Page<Amiibo> search(AmiiboSearchCriteria criteria, Pageable pageable);

    Optional<Amiibo> findById(String id);

    Optional<Amiibo> findByExternalId(String externalId);

    Amiibo save(Amiibo amiibo);

    void deleteById(String id);

    void deleteByOwnerId(String ownerId);

    void updateOwnerName(String ownerId, String ownerName);
}
