package com.wikicollection.domain.port.in;

import java.util.List;

import com.wikicollection.domain.model.MagicCardSearchResult;

public interface DeckSearchUseCase {

    List<MagicCardSearchResult> searchCommanders(String colors);
}
