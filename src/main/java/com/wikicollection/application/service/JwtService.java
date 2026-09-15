package com.wikicollection.application.service;

import java.nio.charset.StandardCharsets;
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

    public JwtService(
            @Value("${app.jwt.secret:clave-cambiar-en-produccion-min-256-bits}") String secret,
            @Value("${app.jwt.access-token-expiration:900000}") long accessExpirationMillis,
            @Value("${app.jwt.refresh-token-expiration:604800000}") long refreshExpirationMillis) {
        this.key = toKey(secret);
        this.accessExpirationMillis = accessExpirationMillis;
        this.refreshExpirationMillis = refreshExpirationMillis;
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
                .claim("type", type)
                .issuedAt(now)
                .expiration(new Date(now.getTime() + expirationMillis))
                .signWith(key)
                .compact();
    }

    private io.jsonwebtoken.Claims parse(String token) {
        return Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();
    }

    private static SecretKey toKey(String secret) {
        byte[] bytes;
        try {
            bytes = Decoders.BASE64.decode(secret);
        } catch (RuntimeException e) {
            bytes = secret.getBytes(StandardCharsets.UTF_8);
        }
        if (bytes.length < 32) {
            throw new IllegalStateException("El secreto JWT debe tener al menos 256 bits");
        }
        return Keys.hmacShaKeyFor(bytes);
    }
}
