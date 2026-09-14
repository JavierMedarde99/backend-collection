package com.wikicollection.domain.port.in;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.wikicollection.domain.model.MagicCard;
import com.wikicollection.domain.model.MagicCardSearchResult;

public interface MagicCardSearchUseCase {

    Page<MagicCardSearchResult> search(String query, Pageable pageable);

    MagicCard findByName(String name);
}
