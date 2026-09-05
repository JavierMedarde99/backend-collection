package com.wikicollection.application.service;

import com.wikicollection.application.exception.GameNotFoundException;
import com.wikicollection.domain.model.Game;
import com.wikicollection.domain.model.GameStatus;
import com.wikicollection.domain.port.in.GameUseCase;
import com.wikicollection.domain.port.out.GameRepository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
public class GameService implements GameUseCase {

    private final GameRepository gameRepository;

    public GameService(GameRepository gameRepository) {
        this.gameRepository = gameRepository;
    }

    @Override
    public Page<Game> findAll(Pageable pageable) {
        return gameRepository.findAll(pageable);
    }

    @Override
    public Page<Game> findByStatus(GameStatus status, Pageable pageable) {
        return gameRepository.findByStatus(status, pageable);
    }

    @Override
    public Game findById(String id) {
        return gameRepository.findById(id)
                .orElseThrow(() -> new GameNotFoundException("Juego no encontrado con id: " + id));
    }

    @Override
    public Game save(Game game) {
        return gameRepository.save(game);
    }

    @Override
    public Game update(String id, Game updates) {
        Game existing = findById(id);
        copyUpdatableFields(existing, updates);
        return gameRepository.save(existing);
    }

    @Override
    public void delete(String id) {
        findById(id);
        gameRepository.deleteById(id);
    }

    private void copyUpdatableFields(Game target, Game source) {
        target.setExternalId(source.getExternalId());
        target.setTitle(source.getTitle());
        target.setDescription(source.getDescription());
        target.setGenre(source.getGenre());
        target.setPlatform(source.getPlatform());
        target.setPublisher(source.getPublisher());
        target.setDeveloper(source.getDeveloper());
        target.setReleaseDate(source.getReleaseDate());
        target.setThumbnailUrl(source.getThumbnailUrl());
        target.setStatus(source.getStatus());
        target.setUserRating(source.getUserRating());
        target.setNotes(source.getNotes());
        target.setDateAdded(source.getDateAdded());
        target.setDateCompleted(source.getDateCompleted());
        target.setExternalSource(source.getExternalSource());
    }
}