package com.wikicollection.domain.port.out;

import java.util.List;
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

    void updateOwnerName(String ownerId, String ownerName);

    List<String> distinctGenres(List<String> excludeOwnerIds);

    /** El juego que este usuario tiene con ese bggId, si lo tiene. */
    Optional<BoardGame> findByOwnerIdAndBggId(String ownerId, String bggId);

    /** Los juegos que este usuario tiene con esos bggIds (para el flag «ya lo tienes»). */
    List<BoardGame> findByOwnerIdAndBggIdIn(String ownerId, List<String> bggIds);

    /** Las expansiones que este usuario tiene enlazadas a un juego base. */
    List<BoardGame> findByOwnerIdAndParentId(String ownerId, String parentId);

    /** Borra en cascada las expansiones de un juego base. */
    void deleteByOwnerIdAndParentId(String ownerId, String parentId);
}
