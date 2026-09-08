package com.wikicollection.domain.port.out;

import java.util.Optional;

import com.wikicollection.domain.model.BoardGame;
import com.wikicollection.domain.model.BoardGameSearchCriteria;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface BoardGameRepository {

    Page<BoardGame> search(BoardGameSearchCriteria criteria, Pageable pageable);

    Optional<BoardGame> findById(String id);

    BoardGame save(BoardGame boardGame);

    void deleteById(String id);
}
