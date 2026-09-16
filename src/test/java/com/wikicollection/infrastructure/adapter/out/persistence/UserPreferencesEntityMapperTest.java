package com.wikicollection.infrastructure.adapter.out.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;
import java.util.Map;

import com.wikicollection.domain.model.CollectionVisibility;
import com.wikicollection.domain.model.UserPreferences;

import org.junit.jupiter.api.Test;

class UserPreferencesEntityMapperTest {

    private final UserPreferencesEntityMapper mapper = new UserPreferencesEntityMapper();

    private UserPreferences sample() {
        return UserPreferences.builder()
                .id("p1")
                .userId("u1")
                .activeCollections(Map.of("books", true, "games", false))
                .collectionVisibility(Map.of("books", CollectionVisibility.PUBLIC, "games", CollectionVisibility.PRIVATE))
                .createdAt(LocalDateTime.of(2026, 1, 1, 0, 0))
                .updatedAt(LocalDateTime.of(2026, 1, 2, 0, 0))
                .build();
    }

    @Test
    void toEntity_convertsVisibilityToNames() {
        UserPreferencesEntity entity = mapper.toEntity(sample());

        assertThat(entity.getUserId()).isEqualTo("u1");
        assertThat(entity.getActiveCollections()).isEqualTo(Map.of("books", true, "games", false));
        assertThat(entity.getCollectionVisibility())
                .isEqualTo(Map.of("books", "PUBLIC", "games", "PRIVATE"));
    }

    @Test
    void toDomain_convertsNamesToVisibility() {
        UserPreferencesEntity entity = mapper.toEntity(sample());

        UserPreferences domain = mapper.toDomain(entity);

        assertThat(domain.getId()).isEqualTo("p1");
        assertThat(domain.getCollectionVisibility())
                .isEqualTo(Map.of("books", CollectionVisibility.PUBLIC, "games", CollectionVisibility.PRIVATE));
        assertThat(domain.getCreatedAt()).isEqualTo(LocalDateTime.of(2026, 1, 1, 0, 0));
    }

    @Test
    void nullSafe_returnsNull() {
        assertThat(mapper.toEntity(null)).isNull();
        assertThat(mapper.toDomain(null)).isNull();
    }
}
