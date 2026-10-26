package com.wikicollection.domain.port.out;

import java.util.List;
import java.util.Optional;

import com.wikicollection.domain.model.Game;
import com.wikicollection.domain.model.GameGroup;
import com.wikicollection.domain.model.GameSearchCriteria;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface GameRepository {

    Page<Game> search(GameSearchCriteria criteria, Pageable pageable);

    /**
     * Listado agrupado por juego: una entrada por {@code gameKey} con una fila por plataforma.
     * La paginación (y el {@code totalElements}) cuenta grupos, no registros.
     */
    Page<GameGroup> searchGrouped(GameSearchCriteria criteria, Pageable pageable);

    Optional<Game> findById(String id);

    Game save(Game game);

    void deleteById(String id);

    void updateOwnerName(String ownerId, String ownerName);

    List<String> distinctGenres(List<String> excludeOwnerIds);

    List<Game> findByOwnerIdAndExternalIdIn(String ownerId, List<String> externalIds);

    void deleteByOwnerIdAndParentId(String ownerId, String parentId);

    void deleteByOwnerId(String ownerId);
}
