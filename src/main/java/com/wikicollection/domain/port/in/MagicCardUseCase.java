package com.wikicollection.domain.port.in;

import com.wikicollection.domain.model.MagicCard;
import com.wikicollection.domain.model.MagicCardSearchCriteria;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface MagicCardUseCase {

    Page<MagicCard> search(MagicCardSearchCriteria criteria, Pageable pageable);

    MagicCard findById(String id);

    MagicCard addFromScryfall(String scryfallId);

    void delete(String id);
}
