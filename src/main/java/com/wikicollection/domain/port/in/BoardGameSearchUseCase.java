package com.wikicollection.domain.port.in;

import java.util.List;

import com.wikicollection.domain.model.BoardGameSearchResult;

public interface BoardGameSearchUseCase {

    List<BoardGameSearchResult> search(String query);
}
