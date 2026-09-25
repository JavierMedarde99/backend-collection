package com.wikicollection.infrastructure.adapter.out.persistence;

import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;

import com.wikicollection.domain.model.MovieSearchCriteria;
import com.wikicollection.domain.model.MovieShow;
import com.wikicollection.domain.port.out.MovieShowRepository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Component;

@Component
public class MovieShowPersistenceAdapter implements MovieShowRepository {

    private final SpringDataMovieShowRepository springDataMovieShowRepository;
    private final MongoTemplate mongoTemplate;
    private final MovieShowEntityMapper mapper;

    public MovieShowPersistenceAdapter(SpringDataMovieShowRepository springDataMovieShowRepository,
                                       MongoTemplate mongoTemplate,
                                       MovieShowEntityMapper mapper) {
        this.springDataMovieShowRepository = springDataMovieShowRepository;
        this.mongoTemplate = mongoTemplate;
        this.mapper = mapper;
    }

    @Override
    public MovieShow save(MovieShow movieShow) {
        MovieShowEntity saved = springDataMovieShowRepository.save(mapper.toEntity(movieShow));
        return mapper.toDomain(saved);
    }

    @Override
    public Optional<MovieShow> findById(String id) {
        return springDataMovieShowRepository.findById(id).map(mapper::toDomain);
    }

    @Override
    public Page<MovieShow> findAll(Pageable pageable) {
        return findByCriteria(null, pageable);
    }

    @Override
    public java.util.List<String> distinctGenres(java.util.List<String> excludeOwnerIds) {
        var distinct = mongoTemplate.query(MovieShowEntity.class).distinct("genres").as(String.class);
        if (excludeOwnerIds != null && !excludeOwnerIds.isEmpty()) {
            return distinct.matching(new Query(Criteria.where("ownerId").nin(excludeOwnerIds))).all();
        }
        return distinct.all();
    }

    @Override
    public Page<MovieShow> findByCriteria(MovieSearchCriteria criteria, Pageable pageable) {
        Query query = buildQuery(criteria);
        long total = mongoTemplate.count(query, MovieShowEntity.class);
        query.with(pageable);
        List<MovieShow> content = mongoTemplate.find(query, MovieShowEntity.class).stream()
                .map(mapper::toDomain)
                .toList();
        return new PageImpl<>(content, pageable, total);
    }

    @Override
    public void deleteById(String id) {
        springDataMovieShowRepository.deleteById(id);
    }

    @Override
    public Optional<MovieShow> findByExternalId(String externalId) {
        return springDataMovieShowRepository.findByExternalId(externalId).map(mapper::toDomain);
    }

    @Override
    public long count() {
        return springDataMovieShowRepository.count();
    }

    private Query buildQuery(MovieSearchCriteria criteria) {
        Query query = new Query();
        if (criteria == null) {
            return query;
        }
        if (criteria.hasOwnerId()) {
            query.addCriteria(Criteria.where("ownerId").is(criteria.ownerId()));
        }
        if (criteria.hasExcludeOwnerIds()) {
            query.addCriteria(Criteria.where("ownerId").nin(criteria.excludeOwnerIds()));
        }
        if (criteria.hasName()) {
            query.addCriteria(Criteria.where("title").regex(ciPattern(criteria.name())));
        }
        if (criteria.status() != null) {
            query.addCriteria(Criteria.where("status").is(criteria.status()));
        }
        if (criteria.mediaType() != null) {
            query.addCriteria(Criteria.where("mediaType").is(criteria.mediaType()));
        }
        if (criteria.hasGenres()) {
            Criteria[] anyGenre = criteria.effectiveGenres().stream()
                    .map(g -> Criteria.where("genres").regex(ciPattern(g)))
                    .toArray(Criteria[]::new);
            query.addCriteria(new Criteria().orOperator(anyGenre));
        }
        return query;
    }

    private Pattern ciPattern(String value) {
        return Pattern.compile(Pattern.quote(value), Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
    }

    @Override
    public void updateOwnerName(String ownerId, String ownerName) {
        mongoTemplate.updateMulti(
                new Query(Criteria.where("ownerId").is(ownerId)),
                new Update().set("userOwned.ownerName", ownerName),
                MovieShowEntity.class);
    }
}
