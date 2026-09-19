package com.wikicollection.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Base64;
import java.util.Optional;

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

    @Mock
    private com.wikicollection.domain.port.out.BookRepository bookRepository;

    @Mock
    private com.wikicollection.domain.port.out.GameRepository gameRepository;

    @Mock
    private com.wikicollection.domain.port.out.BoardGameRepository boardGameRepository;

    @Mock
    private com.wikicollection.domain.port.out.MagicCardRepository magicCardRepository;

    @Mock
    private com.wikicollection.domain.port.out.DeckRepository deckRepository;

    @Mock
    private com.wikicollection.domain.port.out.MovieShowRepository movieShowRepository;

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

        authService.register("javi", "javi@local.dev", "pass123", "Javi", null);

        ArgumentCaptor<UserPreferences> captor = ArgumentCaptor.forClass(UserPreferences.class);
        verify(preferencesRepository).save(captor.capture());
        assertThat(captor.getValue().getUserId()).isEqualTo("u1");
        assertThat(captor.getValue().getActiveCollections()).containsEntry("books", true);
        assertThat(captor.getValue().getCollectionVisibility())
                .containsEntry("books", CollectionVisibility.PUBLIC);
    }

    @Test
    void updateProfile_updatesFieldsAndPropagatesName() {
        User user = User.builder().id("u1").username("javi").displayName("Javi").build();
        when(userRepository.findById("u1")).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        User updated = authService.updateProfile("u1", "Nuevo", null, "bio nueva", null);

        assertThat(updated.getDisplayName()).isEqualTo("Nuevo");
        assertThat(updated.getBio()).isEqualTo("bio nueva");
        assertThat(updated.getUsername()).isEqualTo("javi");
        verify(bookRepository).updateOwnerName("u1", "Nuevo");
        verify(gameRepository).updateOwnerName("u1", "Nuevo");
        verify(deckRepository).updateOwnerName("u1", "Nuevo");
    }

    @Test
    void updateProfile_skipsPropagation_whenNameUnchanged() {
        User user = User.builder().id("u1").username("javi").build();
        when(userRepository.findById("u1")).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        authService.updateProfile("u1", null, "http://avatar", null, null);

        verify(bookRepository, never()).updateOwnerName(any(), any());
    }

    @Test
    void deleteAccount_removesEverything() {
        User user = User.builder().id("u1").username("javi").build();
        when(userRepository.findById("u1")).thenReturn(Optional.of(user));
        com.wikicollection.domain.model.Book book =
                com.wikicollection.domain.model.Book.builder().id("b1").ownerId("u1").build();
        when(bookRepository.search(any(), any()))
                .thenReturn(new org.springframework.data.domain.PageImpl<>(java.util.List.of(book)));
        when(gameRepository.search(any(), any()))
                .thenReturn(org.springframework.data.domain.Page.empty());
        when(boardGameRepository.search(any(), any()))
                .thenReturn(org.springframework.data.domain.Page.empty());
        when(magicCardRepository.search(any(), any()))
                .thenReturn(org.springframework.data.domain.Page.empty());
        when(deckRepository.findByOwnerId(eq("u1"), any()))
                .thenReturn(org.springframework.data.domain.Page.empty());
        when(movieShowRepository.findByCriteria(any(), any()))
                .thenReturn(org.springframework.data.domain.Page.empty());

        authService.deleteAccount("u1");

        verify(bookRepository).deleteById("b1");
        verify(preferencesRepository).deleteByUserId("u1");
        verify(userRepository).deleteById("u1");
    }

    @Test
    void deleteAccount_throwsNotFound_whenMissing() {
        when(userRepository.findById("ghost")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.deleteAccount("ghost"))
                .isInstanceOf(com.wikicollection.application.exception.UserNotFoundException.class);
        verify(userRepository, never()).deleteById(any());
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

        authService.register("javi", "javi@local.dev", "pass123", "Javi", null);

        verify(preferencesRepository, never()).save(any(UserPreferences.class));
    }

}
