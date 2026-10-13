package com.wikicollection.domain.port.in;

import com.wikicollection.domain.model.AmiiboSearchResult;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface AmiiboSearchUseCase {

    Page<AmiiboSearchResult> search(String query, Pageable pageable);
}