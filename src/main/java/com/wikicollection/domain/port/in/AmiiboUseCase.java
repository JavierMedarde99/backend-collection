package com.wikicollection.domain.port.in;

import com.wikicollection.domain.model.Amiibo;
import com.wikicollection.domain.model.AmiiboSearchCriteria;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface AmiiboUseCase {

    Page<Amiibo> search(AmiiboSearchCriteria criteria, Pageable pageable, String owner, String viewerId);

    Amiibo findById(String id);

    Amiibo save(Amiibo amiibo, String ownerId);

    Amiibo update(String id, Amiibo updates, String userId);

    void delete(String id, String userId);
}
