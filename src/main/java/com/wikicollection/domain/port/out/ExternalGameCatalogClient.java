package com.wikicollection.domain.port.out;

import java.util.List;

import com.wikicollection.domain.model.GameSearchResult;

public interface ExternalGameCatalogClient {

    List<GameSearchResult> search(String query);

    List<GameSearchResult> getAllGames();
}