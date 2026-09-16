package com.wikicollection.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.wikicollection.application.exception.UserNotFoundException;
import com.wikicollection.domain.model.CollectionType;
import com.wikicollection.domain.model.CollectionVisibility;
import com.wikicollection.domain.model.User;
import com.wikicollection.domain.model.UserPreferences;
import com.wikicollection.domain.port.out.UserPreferencesRepository;
import com.wikicollection.domain.port.out.UserRepository;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class UserPreferencesServiceTest {

    @Mock
    private UserPreferencesRepository preferencesRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserPreferencesService service;

    private User sampleUser() {
        return User.builder().id("u1").username("javi").build();
    }

    @Test
    void getPreferences_createsDefaults_whenMissing() {
        when(userRepository.findById("u1")).thenReturn(Optional.of(sampleUser()));
        when(preferencesRepository.findByUserId("u1")).thenReturn(Optional.empty());
        when(preferencesRepository.save(any(UserPreferences.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        UserPreferences prefs = service.getPreferences("u1");

        assertThat(prefs.getUserId()).isEqualTo("u1");
        assertThat(prefs.getActiveCollections()).containsEntry("books", true);
        assertThat(prefs.getCollectionVisibility()).containsEntry("books", CollectionVisibility.PUBLIC);
        verify(preferencesRepository).save(any(UserPreferences.class));
    }

    @Test
    void getPreferences_returnsExisting_withoutSaving() {
        when(userRepository.findById("u1")).thenReturn(Optional.of(sampleUser()));
        UserPreferences existing = UserPreferences.defaults("u1");
        when(preferencesRepository.findByUserId("u1")).thenReturn(Optional.of(existing));

        assertThat(service.getPreferences("u1")).isSameAs(existing);
        verify(preferencesRepository, never()).save(any(UserPreferences.class));
    }

    @Test
    void updatePreferences_replacesMaps() {
        when(userRepository.findById("u1")).thenReturn(Optional.of(sampleUser()));
        when(preferencesRepository.findByUserId("u1"))
                .thenReturn(Optional.of(UserPreferences.defaults("u1")));
        when(preferencesRepository.save(any(UserPreferences.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        UserPreferences updated = service.updatePreferences("u1",
                Map.of("books", true), Map.of("books", CollectionVisibility.PRIVATE));

        assertThat(updated.getActiveCollections()).isEqualTo(Map.of("books", true));
        assertThat(updated.getCollectionVisibility())
                .isEqualTo(Map.of("books", CollectionVisibility.PRIVATE));
    }

    @Test
    void updatePreferences_rejectsUnknownCollection() {
        when(userRepository.findById("u1")).thenReturn(Optional.of(sampleUser()));

        assertThatThrownBy(() -> service.updatePreferences("u1",
                        Map.of("comics", true), Map.of()))
                .isInstanceOf(IllegalArgumentException.class);
        verify(preferencesRepository, never()).save(any(UserPreferences.class));
    }

    @Test
    void updatePreferences_rejectsUnknownUser() {
        when(userRepository.findById("ghost")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.updatePreferences("ghost", Map.of(), Map.of()))
                .isInstanceOf(UserNotFoundException.class);
    }

    @Test
    void setActiveCollections_mergesWithExisting() {
        when(userRepository.findById("u1")).thenReturn(Optional.of(sampleUser()));
        when(preferencesRepository.findByUserId("u1"))
                .thenReturn(Optional.of(UserPreferences.defaults("u1")));
        when(preferencesRepository.save(any(UserPreferences.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        UserPreferences updated = service.setActiveCollections("u1", Map.of("books", false));

        assertThat(updated.getActiveCollections())
                .containsEntry("books", false)
                .containsEntry("games", true);
    }

    @Test
    void getActiveCollections_filtersInactive() {
        UserPreferences prefs = UserPreferences.defaults("u1");
        prefs.getActiveCollections().put("games", false);
        when(preferencesRepository.findByUserId("u1")).thenReturn(Optional.of(prefs));

        List<CollectionType> active = service.getActiveCollections("u1");

        assertThat(active).contains(CollectionType.BOOKS).doesNotContain(CollectionType.GAMES);
    }

    @Test
    void visibilityChecks_reflectStoredValues() {
        UserPreferences prefs = UserPreferences.defaults("u1");
        prefs.getCollectionVisibility().put("books", CollectionVisibility.PRIVATE);
        prefs.getActiveCollections().put("games", false);
        when(preferencesRepository.findByUserId("u1")).thenReturn(Optional.of(prefs));

        assertThat(service.isCollectionPublic("u1", CollectionType.BOOKS)).isFalse();
        assertThat(service.isCollectionActive("u1", CollectionType.GAMES)).isFalse();
        assertThat(service.isCollectionActive("u1", CollectionType.BOOKS)).isTrue();
    }

    @Test
    void getUserIdsWithPrivateCollection_delegatesToRepository() {
        when(preferencesRepository.findUserIdsWithPrivateCollection("books"))
                .thenReturn(List.of("u9"));

        assertThat(service.getUserIdsWithPrivateCollection(CollectionType.BOOKS))
                .containsExactly("u9");
    }

    @Test
    void savedPreferences_carryUserId() {
        when(userRepository.findById("u1")).thenReturn(Optional.of(sampleUser()));
        when(preferencesRepository.findByUserId("u1")).thenReturn(Optional.empty());
        ArgumentCaptor<UserPreferences> captor = ArgumentCaptor.forClass(UserPreferences.class);
        when(preferencesRepository.save(captor.capture()))
                .thenAnswer(invocation -> invocation.getArgument(0));

        service.getPreferences("u1");

        assertThat(captor.getValue().getUserId()).isEqualTo("u1");
    }
}
