package com.wikicollection.domain.port.out;

import java.util.Optional;

import com.wikicollection.domain.model.Game;
import com.wikicollection.domain.model.GameStatus;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface GameRepository {

    Page<Game> findAll(Pageable pageable);

    Page<Game> findByStatus(GameStatus status, Pageable pageable);

    Optional<Game> findById(String id);

    Game save(Game game);

    void deleteById(String id);
}