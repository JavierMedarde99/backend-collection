package com.wikicollection.domain.port.in;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.wikicollection.domain.model.MagicCardSearchResult;

public interface DeckSearchUseCase {

    Page<MagicCardSearchResult> searchCommanders(String colors, Pageable pageable);
}
