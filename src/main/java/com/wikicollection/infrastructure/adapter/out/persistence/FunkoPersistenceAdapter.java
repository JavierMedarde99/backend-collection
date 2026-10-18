package com.wikicollection.infrastructure.adapter.out.persistence;

import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;

import com.wikicollection.domain.model.Funko;
import com.wikicollection.domain.model.FunkoSearchCriteria;
import com.wikicollection.domain.port.out.FunkoRepository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Component;

@Component
public class FunkoPersistenceAdapter implements FunkoRepository {

    private final SpringDataFunkoRepository springDataFunkoRepository;
    private final MongoTemplate mongoTemplate;
    private final FunkoEntityMapper mapper;

    public FunkoPersistenceAdapter(SpringDataFunkoRepository springDataFunkoRepository,
                                   MongoTemplate mongoTemplate,
                                   FunkoEntityMapper mapper) {
        this.springDataFunkoRepository = springDataFunkoRepository;
        this.mongoTemplate = mongoTemplate;
        this.mapper = mapper;
    }

    @Override
    public Page<Funko> search(FunkoSearchCriteria criteria, Pageable pageable) {
        Query query = buildQuery(criteria);
        long total = mongoTemplate.count(query, FunkoEntity.class);
        query.with(pageable);
        List<Funko> content = mongoTemplate.find(query, FunkoEntity.class).stream()
                .map(mapper::toDomain)
                .toList();
        return new PageImpl<>(content, pageable, total);
    }

    private Query buildQuery(FunkoSearchCriteria criteria) {
        Query query = new Query();
        if (criteria.hasName()) {
            query.addCriteria(Criteria.where("name").regex(ciPattern(criteria.name())));
        }
        if (criteria.status() != null) {
            query.addCriteria(Criteria.where("status").is(criteria.status()));
        }
        if (criteria.hasOwnerId()) {
            query.addCriteria(Criteria.where("ownerId").is(criteria.ownerId()));
        }
        if (criteria.hasExcludeOwnerIds()) {
            query.addCriteria(Criteria.where("ownerId").nin(criteria.excludeOwnerIds()));
        }
        return query;
    }

    private Pattern ciPattern(String value) {
        return Pattern.compile(Pattern.quote(value), Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
    }

    @Override
    public Optional<Funko> findById(String id) {
        return springDataFunkoRepository.findById(id).map(mapper::toDomain);
    }

    @Override
    public Optional<Funko> findByExternalId(String externalId) {
        return springDataFunkoRepository.findByExternalId(externalId).map(mapper::toDomain);
    }

    @Override
    public Funko save(Funko funko) {
        FunkoEntity saved = springDataFunkoRepository.save(mapper.toEntity(funko));
        return mapper.toDomain(saved);
    }

    @Override
    public void deleteById(String id) {
        springDataFunkoRepository.deleteById(id);
    }

    @Override
    public void deleteByOwnerId(String ownerId) {
        mongoTemplate.remove(new Query(Criteria.where("ownerId").is(ownerId)), FunkoEntity.class);
    }

    @Override
    public void updateOwnerName(String ownerId, String ownerName) {
        mongoTemplate.updateMulti(
                new Query(Criteria.where("ownerId").is(ownerId)),
                new Update().set("userOwned.ownerName", ownerName),
                FunkoEntity.class);
    }
}
