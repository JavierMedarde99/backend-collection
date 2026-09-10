package com.wikicollection.domain.port.out;

import java.util.Optional;

import com.wikicollection.domain.model.MagicCard;
import com.wikicollection.domain.model.MagicCardSearchCriteria;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface MagicCardRepository {

    Page<MagicCard> search(MagicCardSearchCriteria criteria, Pageable pageable);

    Optional<MagicCard> findById(String id);

    MagicCard save(MagicCard magicCard);

    void deleteById(String id);
}
