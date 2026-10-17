package com.wikicollection.application.service;

import java.util.Date;

import javax.crypto.SecretKey;

import com.wikicollection.domain.model.User;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;

@Service
public class JwtService {

    private final SecretKey key;
    private final long accessExpirationMillis;
    private final long refreshExpirationMillis;
    private final String adminUsername;

    public JwtService(
            @Value("${app.jwt.secret:}") String secret,
            @Value("${app.jwt.access-token-expiration:86400000}") long accessExpirationMillis,
            @Value("${app.jwt.refresh-token-expiration:86400000}") long refreshExpirationMillis,
            @Value("${app.admin.username:admin}") String adminUsername) {
        this.key = toKey(secret);
        this.accessExpirationMillis = accessExpirationMillis;
        this.refreshExpirationMillis = refreshExpirationMillis;
        this.adminUsername = adminUsername;
    }

    public String generateAccessToken(User user) {
        return buildToken(user, "access", accessExpirationMillis);
    }

    public String generateRefreshToken(User user) {
        return buildToken(user, "refresh", refreshExpirationMillis);
    }

    public String extractUserId(String token) {
        return parse(token).getSubject();
    }

    public boolean isRefreshToken(String token) {
        return "refresh".equals(parse(token).get("type", String.class));
    }

    public String extractRole(String token) {
        String role = parse(token).get("role", String.class);
        return role != null ? role : "USER";
    }

    public boolean isTokenValid(String token) {
        try {
            parse(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    private String buildToken(User user, String type, long expirationMillis) {
        Date now = new Date();
        return Jwts.builder()
                .subject(user.getId())
                .claim("username", user.getUsername())
                .claim("role", roleFor(user))
                .claim("type", type)
                .issuedAt(now)
                .expiration(new Date(now.getTime() + expirationMillis))
                .signWith(key)
                .compact();
    }

    private String roleFor(User user) {
        return adminUsername != null && adminUsername.equals(user.getUsername()) ? "ADMIN" : "USER";
    }

    private io.jsonwebtoken.Claims parse(String token) {
        return Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();
    }

    private static SecretKey toKey(String secret) {
        if (secret == null || secret.isBlank()) {
            throw new IllegalStateException("Falta la variable de entorno JWT_SECRET: la aplicación no puede "
                    + "arrancar sin un secreto de firma. Genera uno con "
                    + "`openssl rand -base64 48` (>= 256 bits) y expórtalo antes de arrancar.");
        }
        byte[] bytes;
        try {
            bytes = Decoders.BASE64.decode(secret);
        } catch (RuntimeException e) {
            throw new IllegalStateException("El secreto JWT (JWT_SECRET) no es Base64 válido: la aplicación no puede "
                    + "arrancar sin un secreto de firma correcto. Genera uno con `openssl rand -base64 48` "
                    + "(>= 256 bits) y expórtalo antes de arrancar.", e);
        }
        if (bytes.length < 32) {
            throw new IllegalStateException("El secreto JWT (JWT_SECRET) debe tener al menos 256 bits: la "
                    + "aplicación no puede arrancar con un secreto más corto. Genera uno con "
                    + "`openssl rand -base64 48`.");
        }
        return Keys.hmacShaKeyFor(bytes);
    }
}
