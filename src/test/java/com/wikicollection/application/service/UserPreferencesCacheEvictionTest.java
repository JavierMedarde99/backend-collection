package com.wikicollection.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;

import java.util.Map;
import java.util.Optional;

import com.wikicollection.domain.model.CollectionType;
import com.wikicollection.domain.model.CollectionVisibility;
import com.wikicollection.domain.model.UserPreferences;
import com.wikicollection.domain.port.out.UserPreferencesRepository;
import com.wikicollection.domain.port.out.UserRepository;
import com.wikicollection.infrastructure.config.CacheTestSupport;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.CacheManager;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

/**
 * Issue #483: {@code UserPreferencesService} cachea tres lecturas pero ninguna de las tres
 * escrituras las invalidaba, así que los cambios no se veían hasta que la caché expiraba por
 * TTL.
 *
 * <p>Usa el {@link CacheManager} real: un test unitario con {@code @InjectMocks} no pasa por el
 * proxy de caché y no detectaría el bug.
 */
@SpringBootTest(properties = {"spring.data.mongodb.auto-index-creation=false",
        "app.boardgame-status-migration.enabled=false"})
class UserPreferencesCacheEvictionTest {

    private static final String USER = "u1";

    @Autowired
    private UserPreferencesService userPreferencesService;

    @Autowired
    private CacheManager cacheManager;

    @MockitoBean
    private UserPreferencesRepository preferencesRepository;

    @MockitoBean
    private UserRepository userRepository;

    @BeforeEach
    void setUp() {
        CacheTestSupport.clearAll(cacheManager);
        UserPreferences stored = UserPreferences.defaults(USER);
        lenient().when(preferencesRepository.findByUserId(USER)).thenReturn(Optional.of(stored));
        lenient().when(preferencesRepository.save(any(UserPreferences.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        lenient().when(userRepository.findById(USER))
                .thenReturn(Optional.of(com.wikicollection.domain.model.User.builder()
                        .id(USER).username("javi").build()));
    }

    @Test
    void setActiveCollections_invalidatesCachedActiveCollections() {
        assertThat(userPreferencesService.getActiveCollections(USER))
                .contains(CollectionType.BOOKS);

        userPreferencesService.setActiveCollections(USER, Map.of(CollectionType.BOOKS.getKey(), false));

        assertThat(userPreferencesService.getActiveCollections(USER))
                .as("La lista de colecciones activas debe reflejar el cambio en la misma llamada")
                .doesNotContain(CollectionType.BOOKS);
    }

    @Test
    void setActiveCollections_invalidatesCachedPreferenceFlags() {
        assertThat(userPreferencesService.isCollectionActive(USER, CollectionType.BOOKS)).isTrue();

        userPreferencesService.setActiveCollections(USER, Map.of(CollectionType.BOOKS.getKey(), false));

        assertThat(userPreferencesService.isCollectionActive(USER, CollectionType.BOOKS))
                .as("El flag individual de colección activa debe reflejar el cambio")
                .isFalse();
    }

    @Test
    void setActiveCollections_invalidatesCachedDetail() {
        assertThat(userPreferencesService.getPreferences(USER).getActiveCollections())
                .containsEntry(CollectionType.BOOKS.getKey(), true);

        userPreferencesService.setActiveCollections(USER, Map.of(CollectionType.BOOKS.getKey(), false));

        assertThat(userPreferencesService.getPreferences(USER).getActiveCollections())
                .as("El detalle de preferencias debe reflejar el cambio")
                .containsEntry(CollectionType.BOOKS.getKey(), false);
    }

    @Test
    void setCollectionVisibility_invalidatesCachedDetail() {
        assertThat(userPreferencesService.getPreferences(USER).getCollectionVisibility())
                .containsEntry(CollectionType.BOOKS.getKey(), CollectionVisibility.PUBLIC);

        userPreferencesService.setCollectionVisibility(USER,
                Map.of(CollectionType.BOOKS.getKey(), CollectionVisibility.PRIVATE));

        assertThat(userPreferencesService.getPreferences(USER).getCollectionVisibility())
                .as("El detalle de preferencias debe reflejar el cambio de visibilidad")
                .containsEntry(CollectionType.BOOKS.getKey(),
                        CollectionVisibility.PRIVATE);
    }

    @Test
    void updatePreferences_invalidatesCachedActiveCollectionsAndFlags() {
        assertThat(userPreferencesService.getActiveCollections(USER))
                .contains(CollectionType.FUNKOS);

        userPreferencesService.updatePreferences(USER,
                Map.of(CollectionType.FUNKOS.getKey(), false), Map.of());

        assertThat(userPreferencesService.getActiveCollections(USER))
                .as("updatePreferences debe invalidar la lista cacheada")
                .doesNotContain(CollectionType.FUNKOS);
        assertThat(userPreferencesService.isCollectionActive(USER, CollectionType.FUNKOS))
                .as("updatePreferences debe invalidar los flags cacheados")
                .isFalse();
    }
}
