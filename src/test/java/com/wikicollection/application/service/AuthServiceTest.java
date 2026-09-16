package com.wikicollection.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.when;

import java.util.Base64;
import java.util.Optional;

import com.wikicollection.application.exception.EmailAlreadyExistsException;
import com.wikicollection.application.exception.UserAlreadyExistsException;
import com.wikicollection.application.exception.UserNotFoundException;
import com.wikicollection.domain.model.AuthSession;
import com.wikicollection.domain.model.User;
import com.wikicollection.domain.model.CollectionVisibility;
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
class AuthServiceTest {

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

    @Test
    void register_encodesPasswordAndReturnsTokens() {
        when(userRepository.existsByUsername("javi")).thenReturn(false);
        when(userRepository.existsByEmail("javi@local.dev")).thenReturn(false);
        when(passwordEncoder.encode("pass123")).thenReturn("hash");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User saved = invocation.getArgument(0);
            saved.setId("u1");
            return saved;
        });

        AuthSession session = authService.register("javi", "javi@local.dev", "pass123", "Javi");

        assertThat(session.tokens().accessToken()).isNotBlank();
        assertThat(session.tokens().refreshToken()).isNotBlank();
        assertThat(session.user().getUsername()).isEqualTo("javi");
        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        assertThat(captor.getValue().getPassword()).isEqualTo("hash");
        assertThat(captor.getValue().getCreatedAt()).isNotNull();
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

    @Test
    void register_throwsConflict_whenUsernameExists() {
        when(userRepository.existsByUsername("javi")).thenReturn(true);

        assertThatThrownBy(() -> authService.register("javi", "javi@local.dev", "pass123", "Javi"))
                .isInstanceOf(UserAlreadyExistsException.class);
    }

    @Test
    void register_throwsConflict_whenEmailExists() {
        when(userRepository.existsByUsername("javi")).thenReturn(false);
        when(userRepository.existsByEmail("javi@local.dev")).thenReturn(true);

        assertThatThrownBy(() -> authService.register("javi", "javi@local.dev", "pass123", "Javi"))
                .isInstanceOf(EmailAlreadyExistsException.class);
    }

    @Test
    void login_authenticatesAndReturnsTokens() {
        User user = User.builder().id("u1").username("javi").build();
        when(userRepository.findByUsername("javi")).thenReturn(Optional.of(user));

        AuthSession session = authService.login("javi", "pass123");

        assertThat(session.tokens().accessToken()).isNotBlank();
        verify(authenticationManager).authenticate(any());
    }

    @Test
    void login_throwsNotFound_whenUserMissing() {
        when(userRepository.findByUsername("nope")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.login("nope", "pass123"))
                .isInstanceOf(UserNotFoundException.class);
    }

    @Test
    void refresh_rotatesTokens() {
        User user = User.builder().id("u1").username("javi").build();
        String refresh = jwtService.generateRefreshToken(user);
        when(userRepository.findById("u1")).thenReturn(Optional.of(user));

        AuthSession session = authService.refresh(refresh);

        assertThat(session.tokens().accessToken()).isNotBlank();
        assertThat(session.user().getId()).isEqualTo("u1");
    }

    @Test
    void refresh_rejectsAccessToken() {
        User user = User.builder().id("u1").username("javi").build();
        String access = jwtService.generateAccessToken(user);

        assertThatThrownBy(() -> authService.refresh(access))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void refresh_rejectsGarbage() {
        assertThatThrownBy(() -> authService.refresh("no-es-un-token"))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
