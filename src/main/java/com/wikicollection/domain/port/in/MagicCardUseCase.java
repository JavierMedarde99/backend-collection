package com.wikicollection.domain.port.in;

import com.wikicollection.domain.model.MagicCard;
import com.wikicollection.domain.model.MagicCardSearchCriteria;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface MagicCardUseCase {

    Page<MagicCard> search(MagicCardSearchCriteria criteria, Pageable pageable);

    Page<MagicCard> search(MagicCardSearchCriteria criteria, Pageable pageable, String owner, String viewerId);

    MagicCard findById(String id);

    MagicCard addFromScryfall(String scryfallId, int quantity, String ownerId);

    void delete(String id, String userId);
}
