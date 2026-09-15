package com.wikicollection.application.service;

import java.time.LocalDateTime;

import com.wikicollection.application.exception.EmailAlreadyExistsException;
import com.wikicollection.application.exception.UserNotFoundException;
import com.wikicollection.application.exception.UserAlreadyExistsException;
import com.wikicollection.domain.model.AuthSession;
import com.wikicollection.domain.model.AuthTokens;
import com.wikicollection.domain.model.User;
import com.wikicollection.domain.port.in.UserUseCase;
import com.wikicollection.domain.port.out.UserRepository;

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

    public AuthService(UserRepository userRepository,
                       PasswordEncoder passwordEncoder,
                       JwtService jwtService,
                       AuthenticationManager authenticationManager) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.authenticationManager = authenticationManager;
    }

    @Override
    public AuthSession register(String username, String email, String password, String displayName) {
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
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        User saved = userRepository.save(user);
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

    private AuthTokens tokens(User user) {
        return new AuthTokens(jwtService.generateAccessToken(user), jwtService.generateRefreshToken(user));
    }
}
