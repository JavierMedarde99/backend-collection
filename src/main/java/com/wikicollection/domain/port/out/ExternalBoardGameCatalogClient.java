package com.wikicollection.domain.port.out;

import java.util.List;

import com.wikicollection.domain.model.BoardGameSearchResult;

public interface ExternalBoardGameCatalogClient {

    List<BoardGameSearchResult> search(String query);
}
