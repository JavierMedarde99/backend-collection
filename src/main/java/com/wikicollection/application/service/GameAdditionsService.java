package com.wikicollection.application.service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import com.wikicollection.application.exception.GameAdditionsException;
import com.wikicollection.application.exception.GameNotFoundException;
import com.wikicollection.domain.model.Game;
import com.wikicollection.domain.model.GameAddition;
import com.wikicollection.domain.model.GameAdditionSkip;
import com.wikicollection.domain.model.GameAdditionsAddResult;
import com.wikicollection.domain.model.GameAdditionsCatalog;
import com.wikicollection.domain.model.GameAdditionsPage;
import com.wikicollection.domain.model.GameSearchResult;
import com.wikicollection.domain.model.GameStatus;
import com.wikicollection.domain.port.in.GameAdditionsUseCase;
import com.wikicollection.domain.port.out.ExternalGameAdditionsCatalogClient;
import com.wikicollection.domain.port.out.GameRepository;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;

/**
 * Gemelo de {@code BoardGameExpansionService} para el contenido adicional de
 * videojuegos, con las divergencias del diseño: al vincular un juego suelto no se
 * pierde nada y se informa como {@code linked} (no {@code skipped}), el estado del
 * DLC se elige en el alta y un base que no venga de RAWG es 422, no una lista
 * vacía.
 *
 * <p>Este servicio NO cachea {@code inCollection}/{@code alreadyLinked}: solo el
 * catálogo puro de RAWG está cacheado (en {@code RAWGClient}), porque los flags
 * dependen del espectador.
 */
@Service
public class GameAdditionsService implements GameAdditionsUseCase {

    /** Cuántos externalIds se aceptan por petición; coincide con la página de RAWG. */
    static final int MAX_ADDITIONS = 40;

    private static final String RAWG_SOURCE = "RAWG";

    private final GameRepository gameRepository;
    private final OwnershipValidator ownershipValidator;
    private final ExternalGameAdditionsCatalogClient additionsCatalogClient;
    private final OwnerResolver ownerResolver;

    public GameAdditionsService(GameRepository gameRepository,
                                OwnershipValidator ownershipValidator,
                                ExternalGameAdditionsCatalogClient additionsCatalogClient,
                                OwnerResolver ownerResolver) {
        this.gameRepository = gameRepository;
        this.ownershipValidator = ownershipValidator;
        this.additionsCatalogClient = additionsCatalogClient;
        this.ownerResolver = ownerResolver;
    }

    @Override
    public GameAdditionsCatalog additionsOf(String baseId, String viewerId) {
        Game base = requireBase(baseId);
        String baseExternalId = requireRawgExternalId(base);
        GameAdditionsPage page = additionsCatalogClient.getAdditions(baseExternalId);

        // Una sola consulta por petición; los flags se calculan aquí, fuera de la caché del catálogo.
        Map<String, Game> ownedByExternalId = ownedByExternalId(viewerId, page.additions());

        List<GameAddition> additions = page.additions().stream()
                .map(addition -> {
                    Game owned = ownedByExternalId.get(addition.id());
                    boolean alreadyLinked = owned != null && base.getId() != null
                            && base.getId().equals(owned.getParentId());
                    return new GameAddition(
                            addition.id(),
                            addition.title(),
                            addition.platform(),
                            singleGenre(addition),
                            addition.thumbnailUrl(),
                            addition.releaseDate(),
                            addition.externalSource(),
                            owned != null,
                            alreadyLinked);
                })
                .toList();
        return new GameAdditionsCatalog(base.getId(), baseExternalId, additions, page.totalAvailable());
    }

    @Override
    @CacheEvict(cacheNames = {"gameList", "gameDetail", "stats"}, allEntries = true)
    public GameAdditionsAddResult addAdditions(String baseId, List<String> externalIds, GameStatus status,
                                               String userId) {
        // La entrada se valida antes de mirar el base: unos ids inválidos son 400, no 404.
        List<String> requested = validIds(externalIds);
        if (status == null) {
            throw new IllegalArgumentException("Debes indicar el estado de las adiciones");
        }

        Game base = requireBase(baseId);
        ownershipValidator.validateOwner(base.getOwnerId(), userId);
        if (base.getStatus() == GameStatus.WISHLIST) {
            throw new GameAdditionsException(
                    "El juego base está en WISHLIST: no admite contenido adicional");
        }
        String baseExternalId = requireRawgExternalId(base);

        Map<String, GameSearchResult> additionsById = additionsById(
                additionsCatalogClient.getAdditions(baseExternalId).additions());
        // Nada se guarda si algún id no es una adición real del base.
        for (String externalId : requested) {
            if (!additionsById.containsKey(externalId)) {
                throw new GameAdditionsException(
                        "RAWG no lista " + externalId + " como adición del juego " + baseExternalId);
            }
        }

        Map<String, Game> ownedByExternalId = gameRepository
                .findByOwnerIdAndExternalIdIn(userId, requested).stream()
                .filter(game -> game.getExternalId() != null)
                .collect(Collectors.toMap(Game::getExternalId, game -> game, (first, ignored) -> first));

        List<Game> created = new ArrayList<>();
        List<Game> linked = new ArrayList<>();
        List<GameAdditionSkip> skipped = new ArrayList<>();
        for (String externalId : requested) {
            Game owned = ownedByExternalId.get(externalId);
            if (owned == null) {
                created.add(gameRepository.save(toDlc(base, additionsById.get(externalId), status, userId)));
            } else if (owned.getParentId() != null) {
                skipped.add(new GameAdditionSkip(externalId, GameAdditionSkip.REASON_ALREADY_LINKED));
            } else {
                // Vincular un juego suelto solo escribe el enlace: conserva estado, nota, precio...
                owned.setParentId(base.getId());
                owned.setParentTitle(base.getTitle());
                linked.add(gameRepository.save(owned));
            }
        }
        return new GameAdditionsAddResult(created, linked, skipped);
    }

    private Game toDlc(Game base, GameSearchResult addition, GameStatus status, String userId) {
        return Game.builder()
                .ownerId(userId)
                .userOwned(ownerResolver.resolveOwner(userId))
                .externalId(addition.id())
                .title(addition.title())
                .genres(singleGenre(addition))
                .platform(addition.platform() != null && !addition.platform().isBlank()
                        ? addition.platform()
                        : base.getPlatform())
                .thumbnailUrl(addition.thumbnailUrl())
                .status(status)
                .dateAdded(LocalDate.now())
                .externalSource(RAWG_SOURCE)
                .parentId(base.getId())
                .parentTitle(base.getTitle())
                .build();
    }

    private Map<String, Game> ownedByExternalId(String viewerId, List<GameSearchResult> additions) {
        if (viewerId == null || viewerId.isBlank() || additions.isEmpty()) {
            return Map.of();
        }
        List<String> externalIds = additions.stream()
                .map(GameSearchResult::id)
                .filter(Objects::nonNull)
                .toList();
        Map<String, Game> owned = new LinkedHashMap<>();
        for (Game game : gameRepository.findByOwnerIdAndExternalIdIn(viewerId, externalIds)) {
            if (game.getExternalId() != null) {
                owned.putIfAbsent(game.getExternalId(), game);
            }
        }
        return owned;
    }

    private Map<String, GameSearchResult> additionsById(List<GameSearchResult> additions) {
        Map<String, GameSearchResult> byId = new LinkedHashMap<>();
        for (GameSearchResult addition : additions) {
            if (addition.id() != null && !addition.id().isBlank()) {
                byId.putIfAbsent(addition.id(), addition);
            }
        }
        return byId;
    }

    private List<String> singleGenre(GameSearchResult addition) {
        return addition.genre() == null || addition.genre().isBlank()
                ? List.of()
                : List.of(addition.genre());
    }

    /** Ids no vacíos, sin blancos ni duplicados; menos de {@value #MAX_ADDITIONS}+1. */
    private List<String> validIds(List<String> externalIds) {
        if (externalIds == null || externalIds.isEmpty()) {
            throw new IllegalArgumentException("Debes indicar al menos una adición");
        }
        if (externalIds.stream().anyMatch(id -> id == null || id.isBlank())) {
            throw new IllegalArgumentException("Los externalId de las adiciones no pueden estar en blanco");
        }
        List<String> distinct = externalIds.stream().map(String::strip).distinct().toList();
        if (distinct.size() > MAX_ADDITIONS) {
            throw new IllegalArgumentException(
                    "No se pueden añadir más de " + MAX_ADDITIONS + " adiciones a la vez");
        }
        return distinct;
    }

    private String requireRawgExternalId(Game base) {
        if (base.getExternalId() == null || base.getExternalId().isBlank()) {
            throw new GameAdditionsException(
                    "El juego base no tiene externalId: no se pueden consultar sus adiciones");
        }
        if (!RAWG_SOURCE.equalsIgnoreCase(base.getExternalSource())) {
            throw new GameAdditionsException(
                    "El juego base no viene de RAWG: no se pueden consultar sus adiciones");
        }
        return base.getExternalId();
    }

    private Game requireBase(String baseId) {
        return gameRepository.findById(baseId)
                .orElseThrow(() -> new GameNotFoundException("Juego no encontrado con id: " + baseId));
    }
}
