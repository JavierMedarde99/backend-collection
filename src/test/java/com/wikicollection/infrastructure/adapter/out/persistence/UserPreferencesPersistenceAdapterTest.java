package com.wikicollection.infrastructure.adapter.out.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import com.wikicollection.domain.model.UserPreferences;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;

@ExtendWith(MockitoExtension.class)
class UserPreferencesPersistenceAdapterTest {

    @Mock
    private SpringDataUserPreferencesRepository springDataRepository;

    @Mock
    private MongoTemplate mongoTemplate;

    @Mock
    private UserPreferencesEntityMapper mapper;

    @InjectMocks
    private UserPreferencesPersistenceAdapter adapter;

    @Test
    void findByUserId_mapsEntity_whenExists() {
        UserPreferencesEntity entity = UserPreferencesEntity.builder().userId("u1").build();
        UserPreferences expected = UserPreferences.defaults("u1");
        when(springDataRepository.findByUserId("u1")).thenReturn(Optional.of(entity));
        when(mapper.toDomain(entity)).thenReturn(expected);

        assertThat(adapter.findByUserId("u1")).contains(expected);
    }

    @Test
    void findUserIdsWithPrivateCollection_returnsOwnerIds() {
        UserPreferencesEntity entity = UserPreferencesEntity.builder().userId("u9").build();
        when(mongoTemplate.find(any(Query.class), eq(UserPreferencesEntity.class)))
                .thenReturn(List.of(entity));

        assertThat(adapter.findUserIdsWithPrivateCollection("books")).containsExactly("u9");
    }
}
