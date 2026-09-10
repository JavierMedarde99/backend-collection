package com.wikicollection.domain.port.out;

import java.util.List;

import com.wikicollection.domain.model.MagicCard;
import com.wikicollection.domain.model.MagicCardSearchResult;

public interface ExternalMagicCardCatalogClient {

    List<MagicCardSearchResult> search(String query);

    List<MagicCardSearchResult> searchCommanders(String colors);

    MagicCard findById(String id);

    MagicCard findByName(String name);
}
