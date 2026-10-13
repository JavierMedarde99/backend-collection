package com.wikicollection.domain.port.in;

import com.wikicollection.domain.model.Funko;
import com.wikicollection.domain.model.FunkoSearchCriteria;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface FunkoUseCase {

    Page<Funko> search(FunkoSearchCriteria criteria, Pageable pageable, String owner, String viewerId);

    Funko findById(String id);

    Funko save(Funko funko, String ownerId);

    Funko update(String id, Funko updates, String userId);

    void delete(String id, String userId);
}
