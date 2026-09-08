package com.wikicollection.infrastructure.adapter.out.persistence;

import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;

import com.wikicollection.domain.model.BoardGame;
import com.wikicollection.domain.model.BoardGameSearchCriteria;
import com.wikicollection.domain.port.out.BoardGameRepository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Component;

@Component
public class BoardGamePersistenceAdapter implements BoardGameRepository {

    private final SpringDataBoardGameRepository springDataBoardGameRepository;
    private final MongoTemplate mongoTemplate;
    private final BoardGameEntityMapper mapper;

    public BoardGamePersistenceAdapter(SpringDataBoardGameRepository springDataBoardGameRepository,
                                       MongoTemplate mongoTemplate,
                                       BoardGameEntityMapper mapper) {
        this.springDataBoardGameRepository = springDataBoardGameRepository;
        this.mongoTemplate = mongoTemplate;
        this.mapper = mapper;
    }

    @Override
    public Page<BoardGame> search(BoardGameSearchCriteria criteria, Pageable pageable) {
        Query query = buildQuery(criteria);
        long total = mongoTemplate.count(query, BoardGameEntity.class);
        query.with(pageable);
        List<BoardGame> content = mongoTemplate.find(query, BoardGameEntity.class).stream()
                .map(mapper::toDomain)
                .toList();
        return new PageImpl<>(content, pageable, total);
    }

    private Query buildQuery(BoardGameSearchCriteria criteria) {
        Query query = new Query();
        if (criteria.hasName()) {
            query.addCriteria(Criteria.where("title").regex(ciPattern(criteria.name())));
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
    public Optional<BoardGame> findById(String id) {
        return springDataBoardGameRepository.findById(id).map(mapper::toDomain);
    }

    @Override
    public BoardGame save(BoardGame boardGame) {
        BoardGameEntity saved = springDataBoardGameRepository.save(mapper.toEntity(boardGame));
        return mapper.toDomain(saved);
    }

    @Override
    public void deleteById(String id) {
        springDataBoardGameRepository.deleteById(id);
    }
}
