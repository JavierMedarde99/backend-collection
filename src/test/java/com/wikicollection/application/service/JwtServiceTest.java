package com.wikicollection.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Base64;

import com.wikicollection.domain.model.User;

import org.junit.jupiter.api.Test;

class JwtServiceTest {

    private final String secret = Base64.getEncoder().encodeToString("0123456789abcdef0123456789abcdef".getBytes());
    private final JwtService jwtService = new JwtService(secret, 900000, 604800000, "admin");

    private User sampleUser() {
        return User.builder().id("u1").username("javi").build();
    }

    @Test
    void accessToken_carriesUserRole_forRegularUser() {
        assertThat(jwtService.extractRole(jwtService.generateAccessToken(sampleUser()))).isEqualTo("USER");
    }

    @Test
    void accessToken_carriesAdminRole_forAdminUser() {
        User admin = User.builder().id("a1").username("admin").build();

        assertThat(jwtService.extractRole(jwtService.generateAccessToken(admin))).isEqualTo("ADMIN");
    }

    @Test
    void tokens_carryUserIdAndType() {
        String access = jwtService.generateAccessToken(sampleUser());
        String refresh = jwtService.generateRefreshToken(sampleUser());

        assertThat(jwtService.extractUserId(access)).isEqualTo("u1");
        assertThat(jwtService.extractUserId(refresh)).isEqualTo("u1");
        assertThat(jwtService.isRefreshToken(access)).isFalse();
        assertThat(jwtService.isRefreshToken(refresh)).isTrue();
    }

    @Test
    void validTokens_passValidation() {
        assertThat(jwtService.isTokenValid(jwtService.generateAccessToken(sampleUser()))).isTrue();
        assertThat(jwtService.isTokenValid(jwtService.generateRefreshToken(sampleUser()))).isTrue();
    }

    @Test
    void tamperedToken_failsValidation() {
        String tampered = jwtService.generateAccessToken(sampleUser()) + "x";

        assertThat(jwtService.isTokenValid(tampered)).isFalse();
    }

    @Test
    void expiredToken_failsValidation() {
        JwtService instant = new JwtService(secret, -1000, -1000, "admin");

        assertThat(instant.isTokenValid(instant.generateAccessToken(sampleUser()))).isFalse();
    }

    @Test
    void shortSecret_rejected() {
        assertThatThrownBy(() -> new JwtService("corta", 900000, 604800000, "admin"))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void defaultSecret_acceptedAsRawBytes() {
        JwtService withDefault = new JwtService("clave-cambiar-en-produccion-min-256-bits", 900000, 604800000, "admin");

        assertThat(withDefault.isTokenValid(withDefault.generateAccessToken(sampleUser()))).isTrue();
    }
}
