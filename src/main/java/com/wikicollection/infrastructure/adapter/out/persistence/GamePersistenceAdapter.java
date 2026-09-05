package com.wikicollection.infrastructure.adapter.out.persistence;

import java.util.Optional;

import com.wikicollection.domain.model.Game;
import com.wikicollection.domain.model.GameStatus;
import com.wikicollection.domain.port.out.GameRepository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

@Component
public class GamePersistenceAdapter implements GameRepository {

    private final SpringDataGameRepository springDataGameRepository;
    private final GameEntityMapper mapper;

    public GamePersistenceAdapter(SpringDataGameRepository springDataGameRepository, GameEntityMapper mapper) {
        this.springDataGameRepository = springDataGameRepository;
        this.mapper = mapper;
    }

    @Override
    public Page<Game> findAll(Pageable pageable) {
        return springDataGameRepository.findAll(pageable).map(mapper::toDomain);
    }

    @Override
    public Page<Game> findByStatus(GameStatus status, Pageable pageable) {
        return springDataGameRepository.findByStatus(status, pageable).map(mapper::toDomain);
    }

    @Override
    public Optional<Game> findById(String id) {
        return springDataGameRepository.findById(id).map(mapper::toDomain);
    }

    @Override
    public Game save(Game game) {
        GameEntity saved = springDataGameRepository.save(mapper.toEntity(game));
        return mapper.toDomain(saved);
    }

    @Override
    public void deleteById(String id) {
        springDataGameRepository.deleteById(id);
    }
}