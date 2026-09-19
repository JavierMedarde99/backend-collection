package com.wikicollection.infrastructure.adapter.in.web;

import java.net.URI;

import com.wikicollection.domain.model.AuthSession;
import com.wikicollection.domain.port.in.UserUseCase;
import com.wikicollection.infrastructure.adapter.in.web.dto.AuthResponse;
import com.wikicollection.infrastructure.adapter.in.web.dto.LoginRequest;
import com.wikicollection.infrastructure.adapter.in.web.dto.RefreshTokenRequest;
import com.wikicollection.infrastructure.adapter.in.web.dto.RegisterRequest;
import com.wikicollection.infrastructure.adapter.in.web.dto.UserResponse;
import com.wikicollection.infrastructure.adapter.in.web.dto.UpdateProfileRequest;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import com.wikicollection.application.service.UserPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;

import jakarta.validation.Valid;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/v1/auth")
@Validated
@Tag(name = "Autenticación", description = "Registro, login y tokens. Sin endpoint de logout: es client-side (descartar tokens).")
public class AuthController {

    private final UserUseCase userUseCase;

    public AuthController(UserUseCase userUseCase) {
        this.userUseCase = userUseCase;
    }

    /**
     * Registra un usuario y devuelve 201 con los tokens de la nueva sesión.
     *
     * <p>El header {@code Location} apunta a {@code /api/v1/auth/me}: con los tokens
     * de esta misma respuesta ese endpoint resuelve al recurso recién creado,
     * así que la URL identifica al usuario sin exponer su id en la cabecera.
     */
    @PostMapping("/register")
    @Operation(summary = "Registra un usuario")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Usuario registrado"),
            @ApiResponse(responseCode = "400", description = "Datos inválidos"),
            @ApiResponse(responseCode = "409", description = "Username o email en uso")
    })
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request, UriComponentsBuilder ucb) {
        AuthSession session = userUseCase.register(
                request.username(), request.email(), request.password(), request.displayName(), request.steamId());
        URI location = ucb.path("/api/v1/auth/me").build().toUri();
        return ResponseEntity.created(location).body(toResponse(session));
    }

    @PostMapping("/login")
    @Operation(summary = "Inicia sesión")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Sesión iniciada"),
            @ApiResponse(responseCode = "400", description = "Datos inválidos"),
            @ApiResponse(responseCode = "401", description = "Credenciales inválidas")
    })
    public AuthResponse login(@Valid @RequestBody LoginRequest request) {
        return toResponse(userUseCase.login(request.username(), request.password()));
    }

    @PostMapping("/refresh")
    @Operation(summary = "Rota tokens con un refresh token")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Tokens rotados"),
            @ApiResponse(responseCode = "400", description = "Refresh token inválido")
    })
    public AuthResponse refresh(@Valid @RequestBody RefreshTokenRequest request) {
        return toResponse(userUseCase.refresh(request.refreshToken()));
    }

    @GetMapping("/me")
    @Operation(summary = "Usuario autenticado actual")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Usuario encontrado"),
            @ApiResponse(responseCode = "401", description = "No autenticado"),
            @ApiResponse(responseCode = "404", description = "Usuario no encontrado")
    })
    public UserResponse me(@AuthenticationPrincipal UserPrincipal principal) {
        return UserResponse.from(userUseCase.getById(principal.getId()));
    }

    @PatchMapping("/me")
    @Operation(summary = "Edita el perfil del usuario autenticado")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Perfil actualizado"),
            @ApiResponse(responseCode = "400", description = "Datos inválidos"),
            @ApiResponse(responseCode = "401", description = "No autenticado"),
            @ApiResponse(responseCode = "404", description = "Usuario no encontrado")
    })
    public UserResponse updateMe(@Valid @RequestBody UpdateProfileRequest request,
                                 @AuthenticationPrincipal UserPrincipal principal) {
        return UserResponse.from(userUseCase.updateProfile(
                principal.getId(), request.displayName(), request.avatarUrl(), request.bio(), request.steamId()));
    }

    @DeleteMapping("/me")
    @Operation(summary = "Borra la cuenta del usuario autenticado con todos sus elementos")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Cuenta eliminada"),
            @ApiResponse(responseCode = "401", description = "No autenticado"),
            @ApiResponse(responseCode = "404", description = "Usuario no encontrado")
    })
    public ResponseEntity<Void> deleteMe(@AuthenticationPrincipal UserPrincipal principal) {
        userUseCase.deleteAccount(principal.getId());
        return ResponseEntity.noContent().build();
    }

    private AuthResponse toResponse(AuthSession session) {
        return new AuthResponse(
                session.tokens().accessToken(),
                session.tokens().refreshToken(),
                UserResponse.from(session.user()));
    }
}
