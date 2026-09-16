package com.wikicollection.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Base64;

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
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class AuthServiceCreatePreferencesTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private UserPreferencesRepository preferencesRepository;

    private final String secret = Base64.getEncoder()
            .encodeToString("0123456789abcdef0123456789abcdef".getBytes());

    @Spy
    private JwtService jwtService = new JwtService(secret, 900000, 604800000, "admin");

    @InjectMocks
    private AuthService authService;

    private void existingUserStubs() {
        when(userRepository.existsByUsername("javi")).thenReturn(false);
        when(userRepository.existsByEmail("javi@local.dev")).thenReturn(false);
        when(passwordEncoder.encode("pass123")).thenReturn("hash");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User saved = invocation.getArgument(0);
            saved.setId("u1");
            return saved;
        });
    }

    @Test
    void register_createsDefaultPreferences_whenMissing() {
        when(userRepository.existsByUsername("javi")).thenReturn(false);
        when(userRepository.existsByEmail("javi@local.dev")).thenReturn(false);
        when(passwordEncoder.encode("pass123")).thenReturn("hash");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User saved = invocation.getArgument(0);
            saved.setId("u1");
            return saved;
        });
        when(preferencesRepository.existsByUserId("u1")).thenReturn(false);
        when(preferencesRepository.save(any(UserPreferences.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        authService.register("javi", "javi@local.dev", "pass123", "Javi");

        ArgumentCaptor<UserPreferences> captor = ArgumentCaptor.forClass(UserPreferences.class);
        verify(preferencesRepository).save(captor.capture());
        assertThat(captor.getValue().getUserId()).isEqualTo("u1");
        assertThat(captor.getValue().getActiveCollections()).containsEntry("books", true);
        assertThat(captor.getValue().getCollectionVisibility())
                .containsEntry("books", CollectionVisibility.PUBLIC);
    }

    @Test
    void register_skipsPreferences_whenExisting() {
        when(userRepository.existsByUsername("javi")).thenReturn(false);
        when(userRepository.existsByEmail("javi@local.dev")).thenReturn(false);
        when(passwordEncoder.encode("pass123")).thenReturn("hash");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User saved = invocation.getArgument(0);
            saved.setId("u1");
            return saved;
        });
        when(preferencesRepository.existsByUserId("u1")).thenReturn(true);

        authService.register("javi", "javi@local.dev", "pass123", "Javi");

        verify(preferencesRepository, never()).save(any(UserPreferences.class));
    }

}
