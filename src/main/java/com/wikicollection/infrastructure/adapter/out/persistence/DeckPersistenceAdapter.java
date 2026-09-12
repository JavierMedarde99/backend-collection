package com.wikicollection.infrastructure.adapter.out.persistence;

import java.util.Optional;

import com.wikicollection.domain.model.Deck;
import com.wikicollection.domain.port.out.DeckRepository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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
    public Page<Deck> findAll(Pageable pageable) {
        return springDataDeckRepository.findAll(pageable).map(mapper::toDomain);
    }

    @Override
    public Page<Deck> findByName(String name, Pageable pageable) {
        return springDataDeckRepository.findByName(name, pageable).map(mapper::toDomain);
    }
}
