package com.wikicollection.infrastructure.adapter.out.persistence;

import java.util.List;
import java.util.Optional;

import com.wikicollection.domain.model.CollectionVisibility;
import com.wikicollection.domain.model.UserPreferences;
import com.wikicollection.domain.port.out.UserPreferencesRepository;

import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Component;

@Component
public class UserPreferencesPersistenceAdapter implements UserPreferencesRepository {

    private final SpringDataUserPreferencesRepository springDataRepository;
    private final MongoTemplate mongoTemplate;
    private final UserPreferencesEntityMapper mapper;

    public UserPreferencesPersistenceAdapter(SpringDataUserPreferencesRepository springDataRepository,
                                             MongoTemplate mongoTemplate,
                                             UserPreferencesEntityMapper mapper) {
        this.springDataRepository = springDataRepository;
        this.mongoTemplate = mongoTemplate;
        this.mapper = mapper;
    }

    @Override
    public UserPreferences save(UserPreferences preferences) {
        UserPreferencesEntity saved = springDataRepository.save(mapper.toEntity(preferences));
        return mapper.toDomain(saved);
    }

    @Override
    public Optional<UserPreferences> findByUserId(String userId) {
        return springDataRepository.findByUserId(userId).map(mapper::toDomain);
    }

    @Override
    public boolean existsByUserId(String userId) {
        return springDataRepository.existsByUserId(userId);
    }

    @Override
    public void deleteByUserId(String userId) {
        springDataRepository.deleteByUserId(userId);
    }

    @Override
    public List<String> findUserIdsWithPrivateCollection(String collectionKey) {
        Query query = new Query(Criteria.where("collectionVisibility." + collectionKey)
                .is(CollectionVisibility.PRIVATE.name()));
        return mongoTemplate.find(query, UserPreferencesEntity.class).stream()
                .map(UserPreferencesEntity::getUserId)
                .toList();
    }
}
