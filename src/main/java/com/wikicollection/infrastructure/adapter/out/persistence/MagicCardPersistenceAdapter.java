package com.wikicollection.infrastructure.adapter.out.persistence;

import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;

import com.wikicollection.domain.model.MagicCard;
import com.wikicollection.domain.model.MagicCardSearchCriteria;
import com.wikicollection.domain.port.out.MagicCardRepository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Component;

@Component
public class MagicCardPersistenceAdapter implements MagicCardRepository {

    private final SpringDataMagicCardRepository springDataMagicCardRepository;
    private final MongoTemplate mongoTemplate;
    private final MagicCardEntityMapper mapper;

    public MagicCardPersistenceAdapter(SpringDataMagicCardRepository springDataMagicCardRepository,
                                       MongoTemplate mongoTemplate,
                                       MagicCardEntityMapper mapper) {
        this.springDataMagicCardRepository = springDataMagicCardRepository;
        this.mongoTemplate = mongoTemplate;
        this.mapper = mapper;
    }

    @Override
    public Page<MagicCard> search(MagicCardSearchCriteria criteria, Pageable pageable) {
        Query query = buildQuery(criteria);
        long total = mongoTemplate.count(query, MagicCardEntity.class);
        query.with(pageable);
        List<MagicCard> content = mongoTemplate.find(query, MagicCardEntity.class).stream()
                .map(mapper::toDomain)
                .toList();
        return new PageImpl<>(content, pageable, total);
    }

    private Query buildQuery(MagicCardSearchCriteria criteria) {
        Query query = new Query();
        if (criteria.hasName()) {
            query.addCriteria(Criteria.where("name").regex(ciPattern(criteria.name())));
        }
        if (criteria.hasRarity()) {
            query.addCriteria(Criteria.where("rarity").regex(ciPattern(criteria.rarity())));
        }
        if (criteria.hasColor()) {
            query.addCriteria(Criteria.where("colors").regex(ciPattern(criteria.color())));
        }
        if (criteria.hasType()) {
            query.addCriteria(Criteria.where("type").regex(ciPattern(criteria.type())));
        }
        if (criteria.hasConvertedManaCost()) {
            query.addCriteria(Criteria.where("convertedManaCost").is(criteria.convertedManaCost()));
        }
        return query;
    }

    private Pattern ciPattern(String value) {
        return Pattern.compile(Pattern.quote(value), Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
    }

    @Override
    public Optional<MagicCard> findById(String id) {
        return springDataMagicCardRepository.findById(id).map(mapper::toDomain);
    }

    @Override
    public MagicCard save(MagicCard magicCard) {
        MagicCardEntity saved = springDataMagicCardRepository.save(mapper.toEntity(magicCard));
        return mapper.toDomain(saved);
    }

    @Override
    public void deleteById(String id) {
        springDataMagicCardRepository.deleteById(id);
    }
}
