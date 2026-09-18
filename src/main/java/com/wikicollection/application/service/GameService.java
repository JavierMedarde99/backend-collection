package com.wikicollection.application.service;

import com.wikicollection.application.exception.GameNotFoundException;
import com.wikicollection.domain.model.Game;
import com.wikicollection.domain.model.GameSearchCriteria;
import com.wikicollection.domain.port.in.GameUseCase;
import com.wikicollection.domain.port.out.GameRepository;
import com.wikicollection.domain.port.out.SteamCatalogueClient;

import com.wikicollection.domain.model.CollectionType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
public class GameService implements GameUseCase {

    private final GameRepository gameRepository;
    private final SteamCatalogueClient steamCatalogueClient;
    private final DateRangeValidator dateRangeValidator;
    private final OwnershipValidator ownershipValidator;

    private final OwnerScopeResolver ownerScopeResolver;

    private final OwnerResolver ownerResolver;

    public GameService(GameRepository gameRepository, SteamCatalogueClient steamCatalogueClient, DateRangeValidator dateRangeValidator,
                       OwnershipValidator ownershipValidator,
                       OwnerScopeResolver ownerScopeResolver,
                       OwnerResolver ownerResolver) {
        this.gameRepository = gameRepository;
        this.steamCatalogueClient = steamCatalogueClient;
        this.dateRangeValidator = dateRangeValidator;
        this.ownershipValidator = ownershipValidator;
        this.ownerResolver = ownerResolver;
        this.ownerScopeResolver = ownerScopeResolver;
    }

    @Override
    public Page<Game> search(GameSearchCriteria criteria, Pageable pageable) {
        return gameRepository.search(criteria, pageable);
    }

    @Override
    public Page<Game> search(GameSearchCriteria criteria, Pageable pageable, String owner, String viewerId) {
        OwnerScopeResolver.Scope scope = ownerScopeResolver.resolve(CollectionType.GAMES, owner, viewerId);
        return gameRepository.search(new GameSearchCriteria(criteria.name(), criteria.platform(), criteria.status(), scope.ownerId(), scope.excludeOwnerIds()), pageable);
    }

    @Override
    public Game findById(String id) {
        return gameRepository.findById(id)
                .orElseThrow(() -> new GameNotFoundException("Juego no encontrado con id: " + id));
    }

    @Override
    public Game save(Game game, boolean obtainPlatinum, String ownerId) {
        game.setOwnerId(ownerId);
        game.setUserOwned(ownerResolver.resolveOwner(ownerId));
        dateRangeValidator.validate(game.getDateAdded(), game.getDateCompleted());
        resolveSteamAppId(game, obtainPlatinum);
        return gameRepository.save(game);
    }

    @Override
    public Game update(String id, Game updates, boolean obtainPlatinum, String userId) {
        Game existing = findById(id);
        ownershipValidator.validateOwner(existing.getOwnerId(), userId);
        copyUpdatableFields(existing, updates);
        dateRangeValidator.validate(existing.getDateAdded(), existing.getDateCompleted());
        resolveSteamAppId(existing, obtainPlatinum);
        return gameRepository.save(existing);
    }

    @Override
    public void delete(String id, String userId) {
        Game existing = findById(id);
        ownershipValidator.validateOwner(existing.getOwnerId(), userId);
        gameRepository.deleteById(id);
    }

    private void resolveSteamAppId(Game game, boolean obtainPlatinum) {
        if (game.getSteamAppId() != null && !game.getSteamAppId().isBlank()) {
            return;
        }
        if (!obtainPlatinum || game.getTitle() == null || game.getTitle().isBlank()) {
            return;
        }
        Long appId = steamCatalogueClient.searchGameByName(game.getTitle());
        game.setSteamAppId(appId != null ? appId.toString() : null);
    }

    private void copyUpdatableFields(Game target, Game source) {
        target.setExternalId(source.getExternalId());
        target.setTitle(source.getTitle());
        target.setPlatform(source.getPlatform());
        target.setThumbnailUrl(source.getThumbnailUrl());
        target.setStatus(source.getStatus());
        target.setUserRating(source.getUserRating());
        target.setComment(source.getComment());
        target.setDateAdded(source.getDateAdded());
        target.setDateCompleted(source.getDateCompleted());
        target.setExternalSource(source.getExternalSource());
        target.setSteamAppId(source.getSteamAppId());
    }
}