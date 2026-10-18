package com.wikicollection.application.service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import com.wikicollection.application.exception.BoardGameExpansionException;
import com.wikicollection.application.exception.BoardGameNotFoundException;
import com.wikicollection.domain.model.BoardGame;
import com.wikicollection.domain.model.BoardGameExpansion;
import com.wikicollection.domain.model.BoardGameExpansionAddResult;
import com.wikicollection.domain.model.BoardGameExpansionCatalog;
import com.wikicollection.domain.model.BoardGameExpansionInfo;
import com.wikicollection.domain.model.BoardGameExpansionSkip;
import com.wikicollection.domain.model.BoardGameStatus;
import com.wikicollection.domain.model.BoardGameType;
import com.wikicollection.domain.port.in.BoardGameExpansionUseCase;
import com.wikicollection.domain.port.out.BoardGameRepository;
import com.wikicollection.domain.port.out.ExternalBoardGameCatalogClient;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;

@Service
public class BoardGameExpansionService implements BoardGameExpansionUseCase {

    /** Cuántos ids de BGG se piden por llamada a {@code /thing} (máximo de BGG). */
    private static final int MAX_BGG_IDS = 20;

    private final BoardGameRepository boardGameRepository;
    private final OwnershipValidator ownershipValidator;
    private final ExternalBoardGameCatalogClient bggXmlClient;
    private final OwnerResolver ownerResolver;

    public BoardGameExpansionService(BoardGameRepository boardGameRepository,
                                     OwnershipValidator ownershipValidator,
                                     ExternalBoardGameCatalogClient bggXmlClient,
                                     OwnerResolver ownerResolver) {
        this.boardGameRepository = boardGameRepository;
        this.ownershipValidator = ownershipValidator;
        this.bggXmlClient = bggXmlClient;
        this.ownerResolver = ownerResolver;
    }

    @Override
    public BoardGameExpansionCatalog expansionsOf(String baseId, String viewerId) {
        BoardGame base = requireBase(baseId);
        String baseBggId = base.getBggId();
        List<BoardGameExpansionInfo> infos = (baseBggId == null || baseBggId.isBlank())
                ? List.of()
                : bggXmlClient.expansionsOf(baseBggId);

        Set<String> owned = ownedBggIds(viewerId, infos);
        List<BoardGameExpansion> expansions = infos.stream()
                .map(info -> new BoardGameExpansion(
                        info.bggId(),
                        info.title(),
                        info.yearPublished(),
                        info.imageUrl(),
                        info.thumbnailUrl(),
                        owned.contains(info.bggId())))
                .toList();
        return new BoardGameExpansionCatalog(base.getId(), baseBggId, expansions);
    }

    @Override
    @CacheEvict(cacheNames = {"boardgameList", "boardgameDetail", "stats"}, allEntries = true)
    public BoardGameExpansionAddResult addExpansions(String baseId, List<String> bggIds, String userId) {
        BoardGame base = requireBase(baseId);
        ownershipValidator.validateOwner(base.getOwnerId(), userId);
        if (base.getStatus() != BoardGameStatus.OWNED) {
            throw new BoardGameExpansionException(
                    "El juego base debe estar en OWNED para añadirle expansiones; está en " + base.getStatus());
        }

        List<String> requested = distinctIds(bggIds);
        if (requested.isEmpty()) {
            throw new IllegalArgumentException("Debes indicar al menos un bggId en 'bggIds'");
        }

        Set<String> alreadyInCollection = boardGameRepository.findByOwnerIdAndBggIdIn(userId, requested).stream()
                .map(BoardGame::getBggId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        List<String> pending = requested.stream()
                .filter(bggId -> !alreadyInCollection.contains(bggId))
                .toList();

        // Todo se valida antes de guardar nada: una expansión inválida tira la petición entera.
        List<BoardGame> created = pending.isEmpty()
                ? List.of()
                : createExpansions(base, pending, userId);

        List<BoardGameExpansionSkip> skipped = requested.stream()
                .filter(alreadyInCollection::contains)
                .map(bggId -> new BoardGameExpansionSkip(bggId, BoardGameExpansionSkip.REASON_ALREADY_IN_COLLECTION))
                .toList();
        return new BoardGameExpansionAddResult(created, skipped);
    }

    private List<BoardGame> createExpansions(BoardGame base, List<String> bggIds, String userId) {
        String baseBggId = base.getBggId();
        if (baseBggId == null || baseBggId.isBlank()) {
            throw new BoardGameExpansionException(
                    "El juego base no tiene bggId: no se puede comprobar que la expansión le pertenezca");
        }

        Map<String, BoardGameExpansionInfo> infoByBggId = new LinkedHashMap<>();
        for (int from = 0; from < bggIds.size(); from += MAX_BGG_IDS) {
            List<String> chunk = bggIds.subList(from, Math.min(from + MAX_BGG_IDS, bggIds.size()));
            bggXmlClient.findByBggIds(chunk)
                    .forEach(info -> infoByBggId.put(info.bggId(), info));
        }

        List<BoardGame> toCreate = new ArrayList<>();
        for (String bggId : bggIds) {
            BoardGameExpansionInfo info = infoByBggId.get(bggId);
            if (info == null) {
                throw new BoardGameExpansionException("BGG no conoce una expansión con bggId " + bggId);
            }
            if (info.type() != BoardGameType.BOARDGAME_EXPANSION) {
                throw new BoardGameExpansionException("El bggId " + bggId + " no es una expansión en BGG");
            }
            if (!info.baseBggIds().contains(baseBggId)) {
                throw new BoardGameExpansionException(
                        "La expansión " + bggId + " en BGG cuelga de otra base, no de " + baseBggId);
            }
            toCreate.add(toExpansionGame(base, info, userId));
        }

        return toCreate.stream().map(boardGameRepository::save).toList();
    }

    private BoardGame toExpansionGame(BoardGame base, BoardGameExpansionInfo info, String userId) {
        return BoardGame.builder()
                .ownerId(userId)
                .userOwned(ownerResolver.resolveOwner(userId))
                .title(info.title())
                .genres(List.of())
                .description(info.description())
                .yearPublished(info.yearPublished())
                .minPlayers(info.minPlayers())
                .maxPlayers(info.maxPlayers())
                .minPlaytime(info.minPlaytime())
                .maxPlaytime(info.maxPlaytime())
                .publisher(info.publisher())
                .designers(info.designers())
                .categories(info.categories())
                .mechanics(info.mechanics())
                .imageUrl(info.imageUrl())
                .thumbnailUrl(info.thumbnailUrl())
                .bggRating(info.bggRating())
                .bggId(info.bggId())
                .status(BoardGameStatus.OWNED)
                .dateAdded(java.time.LocalDate.now())
                .bggType(BoardGameType.BOARDGAME_EXPANSION)
                .parentId(base.getId())
                .parentTitle(base.getTitle())
                .build();
    }

    private Set<String> ownedBggIds(String viewerId, List<BoardGameExpansionInfo> infos) {
        if (viewerId == null || viewerId.isBlank() || infos.isEmpty()) {
            return Set.of();
        }
        List<String> bggIds = infos.stream()
                .map(BoardGameExpansionInfo::bggId)
                .filter(Objects::nonNull)
                .toList();
        return boardGameRepository.findByOwnerIdAndBggIdIn(viewerId, bggIds).stream()
                .map(BoardGame::getBggId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
    }

    private List<String> distinctIds(List<String> bggIds) {
        if (bggIds == null) {
            return List.of();
        }
        return bggIds.stream()
                .filter(bggId -> bggId != null && !bggId.isBlank())
                .distinct()
                .toList();
    }

    private BoardGame requireBase(String baseId) {
        return boardGameRepository.findById(baseId)
                .orElseThrow(() -> new BoardGameNotFoundException("Juego de mesa no encontrado con id: " + baseId));
    }
}
