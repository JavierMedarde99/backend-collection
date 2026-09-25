package com.wikicollection.application.service;

import java.time.LocalDateTime;

import com.wikicollection.application.exception.EmailAlreadyExistsException;
import com.wikicollection.application.exception.UserNotFoundException;
import com.wikicollection.application.exception.UserAlreadyExistsException;
import com.wikicollection.domain.model.AuthSession;
import com.wikicollection.domain.model.AuthTokens;
import com.wikicollection.domain.model.BoardGameSearchCriteria;
import com.wikicollection.domain.model.BookSearchCriteria;
import com.wikicollection.domain.model.GameSearchCriteria;
import com.wikicollection.domain.model.MagicCardSearchCriteria;
import com.wikicollection.domain.model.MovieSearchCriteria;
import com.wikicollection.domain.model.User;
import com.wikicollection.domain.model.UserPreferences;
import com.wikicollection.domain.port.in.UserUseCase;
import com.wikicollection.domain.port.out.BoardGameRepository;
import com.wikicollection.domain.port.out.BookRepository;
import com.wikicollection.domain.port.out.DeckRepository;
import com.wikicollection.domain.port.out.GameRepository;
import com.wikicollection.domain.port.out.MagicCardRepository;
import com.wikicollection.domain.port.out.MovieShowRepository;
import com.wikicollection.domain.port.out.UserPreferencesRepository;
import com.wikicollection.domain.port.out.UserRepository;

import org.springframework.data.domain.Pageable;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService implements UserUseCase {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;
    private final UserPreferencesRepository preferencesRepository;
    private final BookRepository bookRepository;
    private final GameRepository gameRepository;
    private final BoardGameRepository boardGameRepository;
    private final MagicCardRepository magicCardRepository;
    private final DeckRepository deckRepository;
    private final MovieShowRepository movieShowRepository;

    public AuthService(UserRepository userRepository,
                       PasswordEncoder passwordEncoder,
                       JwtService jwtService,
                       AuthenticationManager authenticationManager,
                       UserPreferencesRepository preferencesRepository,
                       BookRepository bookRepository,
                       GameRepository gameRepository,
                       BoardGameRepository boardGameRepository,
                       MagicCardRepository magicCardRepository,
                       DeckRepository deckRepository,
                       MovieShowRepository movieShowRepository) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.authenticationManager = authenticationManager;
        this.preferencesRepository = preferencesRepository;
        this.bookRepository = bookRepository;
        this.gameRepository = gameRepository;
        this.boardGameRepository = boardGameRepository;
        this.magicCardRepository = magicCardRepository;
        this.deckRepository = deckRepository;
        this.movieShowRepository = movieShowRepository;
    }

    @Override
    public AuthSession register(String username, String email, String password, String displayName, String steamId) {
        if (userRepository.existsByUsername(username)) {
            throw new UserAlreadyExistsException("El username ya está en uso: " + username);
        }
        if (userRepository.existsByEmail(email)) {
            throw new EmailAlreadyExistsException("El email ya está en uso: " + email);
        }
        User user = User.builder()
                .username(username)
                .email(email)
                .password(passwordEncoder.encode(password))
                .displayName(displayName)
                .steamId(steamId)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        User saved = userRepository.save(user);
        if (!preferencesRepository.existsByUserId(saved.getId())) {
            preferencesRepository.save(UserPreferences.defaults(saved.getId()));
        }
        return new AuthSession(saved, tokens(saved));
    }

    @Override
    public AuthSession login(String username, String password) {
        authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(username, password));
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UserNotFoundException("Usuario no encontrado: " + username));
        return new AuthSession(user, tokens(user));
    }

    @Override
    public AuthSession refresh(String refreshToken) {
        if (!jwtService.isTokenValid(refreshToken) || !jwtService.isRefreshToken(refreshToken)) {
            throw new IllegalArgumentException("Refresh token inválido");
        }
        User user = userRepository.findById(jwtService.extractUserId(refreshToken))
                .orElseThrow(() -> new UserNotFoundException("Usuario no encontrado"));
        return new AuthSession(user, tokens(user));
    }

    @Override
    public User getById(String userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("Usuario no encontrado: " + userId));
    }

    @Override
    public User updateProfile(String userId, String displayName, String avatarUrl, String bio, String steamId) {
        User user = getById(userId);
        if (displayName != null) {
            user.setDisplayName(displayName);
        }
        if (avatarUrl != null) {
            user.setAvatarUrl(avatarUrl);
        }
        if (bio != null) {
            user.setBio(bio);
        }
        if (steamId != null) {
            user.setSteamId(steamId.isBlank() ? null : steamId);
        }
        user.setUpdatedAt(LocalDateTime.now());
        User saved = userRepository.save(user);
        if (displayName != null) {
            propagateOwnerName(userId, displayName);
        }
        return saved;
    }

    @Override
    public void deleteAccount(String userId) {
        getById(userId);
        Pageable all = Pageable.unpaged();
        bookRepository.search(new BookSearchCriteria(null, null, null, null, null, userId, null), all)
                .forEach(book -> bookRepository.deleteById(book.getId()));
        gameRepository.search(new GameSearchCriteria(null, null, null, null, userId, null), all)
                .forEach(game -> gameRepository.deleteById(game.getId()));
        boardGameRepository.search(new BoardGameSearchCriteria(null, null, null, userId, null), all)
                .forEach(boardGame -> boardGameRepository.deleteById(boardGame.getId()));
        magicCardRepository.search(new MagicCardSearchCriteria(null, null, null, null, userId, null), all)
                .forEach(card -> magicCardRepository.deleteById(card.getId()));
        deckRepository.findByOwnerId(userId, all)
                .forEach(deck -> deckRepository.deleteById(deck.getId()));
        movieShowRepository.findByCriteria(new MovieSearchCriteria(null, null, null, null, userId, null), all)
                .forEach(movieShow -> movieShowRepository.deleteById(movieShow.getId()));
        preferencesRepository.deleteByUserId(userId);
        userRepository.deleteById(userId);
    }

    private void propagateOwnerName(String userId, String ownerName) {
        bookRepository.updateOwnerName(userId, ownerName);
        gameRepository.updateOwnerName(userId, ownerName);
        boardGameRepository.updateOwnerName(userId, ownerName);
        magicCardRepository.updateOwnerName(userId, ownerName);
        deckRepository.updateOwnerName(userId, ownerName);
        movieShowRepository.updateOwnerName(userId, ownerName);
    }

    private AuthTokens tokens(User user) {
        return new AuthTokens(jwtService.generateAccessToken(user), jwtService.generateRefreshToken(user));
    }
}
