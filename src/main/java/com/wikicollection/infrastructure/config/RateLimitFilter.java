package com.wikicollection.infrastructure.config;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Limita los endpoints de autenticación a N peticiones/minuto por IP
 * (wiki: 100) para mitigar fuerza bruta. Responde 429 al superar el límite.
 */
@Component
public class RateLimitFilter extends OncePerRequestFilter {

    private static final long WINDOW_MILLIS = 60_000;

    private final int maxPerMinute;
    private final Cache<String, Window> windows = Caffeine.newBuilder()
            .expireAfterWrite(70, TimeUnit.SECONDS)
            .build();

    public RateLimitFilter(@Value("${app.rate-limit.auth-per-minute:100}") int maxPerMinute) {
        this.maxPerMinute = maxPerMinute;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !request.getRequestURI().startsWith("/api/v1/auth/");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        Window window = windows.get(clientIp(request), key -> new Window());
        boolean allowed;
        synchronized (window) {
            long now = System.currentTimeMillis();
            if (now - window.start > WINDOW_MILLIS) {
                window.start = now;
                window.count = 0;
            }
            allowed = window.count < maxPerMinute;
            if (allowed) {
                window.count++;
            }
        }
        if (!allowed) {
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.getWriter().write("""
                    {"timestamp":"%s","status":429,"error":"Too Many Requests",\
                    "message":"Demasiadas peticiones, inténtalo más tarde","path":"%s"}\
                    """.formatted(LocalDateTime.now(), request.getRequestURI()));
            return;
        }
        chain.doFilter(request, response);
    }

    private static String clientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    private static class Window {
        long start = System.currentTimeMillis();
        int count;
    }
}
