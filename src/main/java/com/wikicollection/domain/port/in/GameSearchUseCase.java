package com.wikicollection.domain.port.in;

import java.util.List;

import com.wikicollection.domain.model.GameSearchResult;

public interface GameSearchUseCase {

    List<GameSearchResult> search(String query);
}