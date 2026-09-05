package com.wikicollection.infrastructure.adapter.out.persistence;

import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;

import com.wikicollection.domain.model.Game;
import com.wikicollection.domain.model.GameSearchCriteria;
import com.wikicollection.domain.port.out.GameRepository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Component;

@Component
public class GamePersistenceAdapter implements GameRepository {

    private final SpringDataGameRepository springDataGameRepository;
    private final MongoTemplate mongoTemplate;
    private final GameEntityMapper mapper;

    public GamePersistenceAdapter(SpringDataGameRepository springDataGameRepository,
                                  MongoTemplate mongoTemplate,
                                  GameEntityMapper mapper) {
        this.springDataGameRepository = springDataGameRepository;
        this.mongoTemplate = mongoTemplate;
        this.mapper = mapper;
    }

    @Override
    public Page<Game> search(GameSearchCriteria criteria, Pageable pageable) {
        Query query = buildQuery(criteria);
        long total = mongoTemplate.count(query, GameEntity.class);
        query.with(pageable);
        List<Game> content = mongoTemplate.find(query, GameEntity.class).stream()
                .map(mapper::toDomain)
                .toList();
        return new PageImpl<>(content, pageable, total);
    }

    private Query buildQuery(GameSearchCriteria criteria) {
        Query query = new Query();
        if (criteria.hasName()) {
            query.addCriteria(Criteria.where("title").regex(ciPattern(criteria.name())));
        }
        if (criteria.platform() != null) {
            query.addCriteria(Criteria.where("platform").is(criteria.platform()));
        }
        if (criteria.status() != null) {
            query.addCriteria(Criteria.where("status").is(criteria.status()));
        }
        return query;
    }

    private Pattern ciPattern(String value) {
        return Pattern.compile(Pattern.quote(value), Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
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