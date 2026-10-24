package com.wikicollection.domain.port.out;

import java.util.List;
import java.util.Optional;

import com.wikicollection.domain.model.Game;
import com.wikicollection.domain.model.GameSearchCriteria;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface GameRepository {

    Page<Game> search(GameSearchCriteria criteria, Pageable pageable);

    /**
     * Listado agrupado por juego: una entrada por {@code gameKey} con todas sus filas por
     * plataforma. La paginación y el total cuentan grupos, no registros; los filtros se aplican
     * antes de agrupar.
     */
    Page<com.wikicollection.domain.model.GameGroup> searchGrouped(GameSearchCriteria criteria, Pageable pageable);

    Optional<Game> findById(String id);

    Game save(Game game);

    void deleteById(String id);

    void deleteByOwnerId(String ownerId);

    void updateOwnerName(String ownerId, String ownerName);

    List<String> distinctGenres(List<String> excludeOwnerIds);

    /**
     * Los juegos que este usuario tiene con esos externalId (para el flag «ya lo
     * tienes») del catálogo externo. Acotado por dueño a propósito: {@code
     * findByExternalId} no filtra por dueño y serviría datos de otra colección.
     */
    List<Game> findByOwnerIdAndExternalIdIn(String ownerId, List<String> externalIds);

    /** Borra en cascada las adiciones (DLC) de un juego base. */
    void deleteByOwnerIdAndParentId(String ownerId, String parentId);
}
