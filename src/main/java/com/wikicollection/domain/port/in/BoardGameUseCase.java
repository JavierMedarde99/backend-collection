package com.wikicollection.domain.port.in;

import com.wikicollection.domain.model.BoardGame;
import com.wikicollection.domain.model.BoardGameSearchCriteria;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface BoardGameUseCase {

    Page<BoardGame> search(BoardGameSearchCriteria criteria, Pageable pageable);

    BoardGame findById(String id);

    BoardGame save(BoardGame boardGame, String ownerId);

    BoardGame update(String id, BoardGame updates, String userId);

    void delete(String id, String userId);
}
