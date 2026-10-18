package com.wikicollection.infrastructure.adapter.out.persistence;

import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;

import com.wikicollection.domain.model.Amiibo;
import com.wikicollection.domain.model.AmiiboSearchCriteria;
import com.wikicollection.domain.port.out.AmiiboRepository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Component;

@Component
public class AmiiboPersistenceAdapter implements AmiiboRepository {

    private final SpringDataAmiiboRepository springDataAmiiboRepository;
    private final MongoTemplate mongoTemplate;
    private final AmiiboEntityMapper mapper;

    public AmiiboPersistenceAdapter(SpringDataAmiiboRepository springDataAmiiboRepository,
                                    MongoTemplate mongoTemplate,
                                    AmiiboEntityMapper mapper) {
        this.springDataAmiiboRepository = springDataAmiiboRepository;
        this.mongoTemplate = mongoTemplate;
        this.mapper = mapper;
    }

    @Override
    public Page<Amiibo> search(AmiiboSearchCriteria criteria, Pageable pageable) {
        Query query = buildQuery(criteria);
        long total = mongoTemplate.count(query, AmiiboEntity.class);
        query.with(pageable);
        List<Amiibo> content = mongoTemplate.find(query, AmiiboEntity.class).stream()
                .map(mapper::toDomain)
                .toList();
        return new PageImpl<>(content, pageable, total);
    }

    private Query buildQuery(AmiiboSearchCriteria criteria) {
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
    public Optional<Amiibo> findById(String id) {
        return springDataAmiiboRepository.findById(id).map(mapper::toDomain);
    }

    @Override
    public Optional<Amiibo> findByExternalId(String externalId) {
        return springDataAmiiboRepository.findByExternalId(externalId).map(mapper::toDomain);
    }

    @Override
    public Amiibo save(Amiibo amiibo) {
        AmiiboEntity saved = springDataAmiiboRepository.save(mapper.toEntity(amiibo));
        return mapper.toDomain(saved);
    }

    @Override
    public void deleteById(String id) {
        springDataAmiiboRepository.deleteById(id);
    }

    @Override
    public void deleteByOwnerId(String ownerId) {
        mongoTemplate.remove(new Query(Criteria.where("ownerId").is(ownerId)), AmiiboEntity.class);
    }

    @Override
    public void updateOwnerName(String ownerId, String ownerName) {
        mongoTemplate.updateMulti(
                new Query(Criteria.where("ownerId").is(ownerId)),
                new Update().set("userOwned.ownerName", ownerName),
                AmiiboEntity.class);
    }
}
