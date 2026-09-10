package com.wikicollection.infrastructure.adapter.out.persistence;

import java.util.List;
import java.util.Optional;

import com.wikicollection.domain.model.Deck;
import com.wikicollection.domain.port.out.DeckRepository;

import org.springframework.stereotype.Component;

@Component
public class DeckPersistenceAdapter implements DeckRepository {

    private final SpringDataDeckRepository springDataDeckRepository;
    private final DeckEntityMapper mapper;

    public DeckPersistenceAdapter(SpringDataDeckRepository springDataDeckRepository,
                                  DeckEntityMapper mapper) {
        this.springDataDeckRepository = springDataDeckRepository;
        this.mapper = mapper;
    }

    @Override
    public Optional<Deck> findById(String id) {
        return springDataDeckRepository.findById(id).map(mapper::toDomain);
    }

    @Override
    public Deck save(Deck deck) {
        DeckEntity saved = springDataDeckRepository.save(mapper.toEntity(deck));
        return mapper.toDomain(saved);
    }

    @Override
    public void deleteById(String id) {
        springDataDeckRepository.deleteById(id);
    }

    @Override
    public List<Deck> findAll() {
        return springDataDeckRepository.findAll().stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public List<Deck> findByName(String name) {
        return springDataDeckRepository.findByName(name).stream()
                .map(mapper::toDomain)
                .toList();
    }
}
