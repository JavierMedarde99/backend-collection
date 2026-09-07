package com.wikicollection.application.service;

import com.wikicollection.application.exception.GameNotFoundException;
import com.wikicollection.domain.model.Game;
import com.wikicollection.domain.model.GameSearchCriteria;
import com.wikicollection.domain.port.in.GameUseCase;
import com.wikicollection.domain.port.out.GameRepository;
import com.wikicollection.domain.port.out.SteamCatalogueClient;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
public class GameService implements GameUseCase {

    private final GameRepository gameRepository;
    private final SteamCatalogueClient steamCatalogueClient;

    public GameService(GameRepository gameRepository, SteamCatalogueClient steamCatalogueClient) {
        this.gameRepository = gameRepository;
        this.steamCatalogueClient = steamCatalogueClient;
    }

    @Override
    public Page<Game> search(GameSearchCriteria criteria, Pageable pageable) {
        return gameRepository.search(criteria, pageable);
    }

    @Override
    public Game findById(String id) {
        return gameRepository.findById(id)
                .orElseThrow(() -> new GameNotFoundException("Juego no encontrado con id: " + id));
    }

    @Override
    public Game save(Game game, boolean obtainPlatinum) {
        resolveSteamAppId(game, obtainPlatinum);
        return gameRepository.save(game);
    }

    @Override
    public Game update(String id, Game updates, boolean obtainPlatinum) {
        Game existing = findById(id);
        copyUpdatableFields(existing, updates);
        resolveSteamAppId(existing, obtainPlatinum);
        return gameRepository.save(existing);
    }

    @Override
    public void delete(String id) {
        findById(id);
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