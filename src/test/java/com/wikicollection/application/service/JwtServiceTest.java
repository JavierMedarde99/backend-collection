package com.wikicollection.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.charset.StandardCharsets;
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
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("base64");
    }

    @Test
    void shortButValidBase64Secret_rejected() {
        String shortBase64 = Base64.getEncoder().encodeToString("corta".getBytes());

        assertThatThrownBy(() -> new JwtService(shortBase64, 900000, 604800000, "admin"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("256");
    }

    @Test
    void blankSecret_rejectedWithActionableMessage() {
        assertThatThrownBy(() -> new JwtService("", 900000, 604800000, "admin"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("JWT_SECRET");
    }

    @Test
    void whitespaceOnlySecret_rejectedWithActionableMessage() {
        assertThatThrownBy(() -> new JwtService("   ", 900000, 604800000, "admin"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("JWT_SECRET");
    }

    @Test
    void nonBase64Secret_rejectedInsteadOfBeingAcceptedAsRawBytes() {
        assertThatThrownBy(() -> new JwtService("clave-cambiar-en-produccion-min-256-bits", 900000, 604800000, "admin"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("base64");
    }

    @Test
    void applicationProperties_declaresNoDefaultJwtSecret() throws Exception {
        String properties = new String(
                JwtService.class.getResourceAsStream("/application.properties").readAllBytes(),
                StandardCharsets.UTF_8);

        assertThat(properties)
                .as("El secreto JWT no puede tener valor por defecto hardcodeado (#476)")
                .contains("app.jwt.secret=${JWT_SECRET:}");
    }
}
