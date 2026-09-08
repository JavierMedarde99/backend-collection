package com.wikicollection.domain.port.in;

import java.util.List;

import com.wikicollection.domain.model.MagicCard;
import com.wikicollection.domain.model.MagicCardSearchResult;

public interface MagicCardSearchUseCase {

    List<MagicCardSearchResult> search(String query);

    MagicCard findByName(String name);
}
