package com.wikicollection.domain.port.in;

import com.wikicollection.domain.model.Game;
import com.wikicollection.domain.model.GameSearchCriteria;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface GameUseCase {

    Page<Game> search(GameSearchCriteria criteria, Pageable pageable);

    java.util.List<String> distinctGenres();

    Page<Game> search(GameSearchCriteria criteria, Pageable pageable, String owner, String viewerId);

    Game findById(String id);

    Game save(Game game, boolean obtainPlatinum, String ownerId);

    Game update(String id, Game updates, boolean obtainPlatinum, String userId);

    void delete(String id, String userId);
}