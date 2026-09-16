package com.wikicollection.infrastructure.config;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import com.wikicollection.domain.model.User;
import com.wikicollection.domain.model.UserPreferences;
import com.wikicollection.domain.port.out.UserPreferencesRepository;
import com.wikicollection.domain.port.out.UserRepository;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class UserPreferencesMigrationTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserPreferencesRepository preferencesRepository;

    private UserPreferencesMigration migration(boolean enabled) {
        return new UserPreferencesMigration(userRepository, preferencesRepository, enabled);
    }

    @Test
    void run_createsDefaults_forUsersWithoutPreferences() {
        User user = User.builder().id("u1").username("javi").build();
        when(userRepository.findAll()).thenReturn(List.of(user));
        when(preferencesRepository.existsByUserId("u1")).thenReturn(false);
        when(preferencesRepository.save(any(UserPreferences.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        migration(true).run(null);

        verify(preferencesRepository).save(any(UserPreferences.class));
    }

    @Test
    void run_skipsUsersWithPreferences() {
        User user = User.builder().id("u1").username("javi").build();
        when(userRepository.findAll()).thenReturn(List.of(user));
        when(preferencesRepository.existsByUserId("u1")).thenReturn(true);

        migration(true).run(null);

        verify(preferencesRepository, never()).save(any(UserPreferences.class));
    }

    @Test
    void run_doesNothing_whenDisabled() {
        migration(false).run(null);

        verify(userRepository, never()).findAll();
    }
}
