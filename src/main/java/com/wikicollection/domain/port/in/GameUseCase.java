package com.wikicollection.domain.port.in;

import com.wikicollection.domain.model.Game;
import com.wikicollection.domain.model.GameStatus;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface GameUseCase {

    Page<Game> findAll(Pageable pageable);

    Page<Game> findByStatus(GameStatus status, Pageable pageable);

    Game findById(String id);

    Game save(Game game);

    Game update(String id, Game updates);

    void delete(String id);
}