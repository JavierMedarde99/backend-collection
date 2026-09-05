package com.wikicollection.domain.port.in;

import com.wikicollection.domain.model.Game;
import com.wikicollection.domain.model.GameSearchCriteria;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface GameUseCase {

    Page<Game> search(GameSearchCriteria criteria, Pageable pageable);

    Game findById(String id);

    Game save(Game game);

    Game update(String id, Game updates);

    void delete(String id);
}