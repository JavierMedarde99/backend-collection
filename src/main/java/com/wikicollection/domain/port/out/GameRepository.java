package com.wikicollection.domain.port.out;

import java.util.Optional;

import com.wikicollection.domain.model.Game;
import com.wikicollection.domain.model.GameSearchCriteria;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface GameRepository {

    Page<Game> search(GameSearchCriteria criteria, Pageable pageable);

    Optional<Game> findById(String id);

    Game save(Game game);

    void deleteById(String id);
}