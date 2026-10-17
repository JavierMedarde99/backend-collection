package com.wikicollection.infrastructure.config;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Limita los endpoints de autenticación a N peticiones/minuto por IP
 * (wiki: 100) para mitigar fuerza bruta. Responde 429 al superar el límite.
 *
 * <p>La IP del cliente se toma de {@code X-Forwarded-For} <strong>solo</strong> cuando la
 * petición llega de un proxy de confianza declarado en {@code app.rate-limit.trusted-proxies}.
 * Sin esa configuración, la cabecera se ignora por completo: es controlada por el cliente y
 * usarla como clave permitiría eludir el límite enviando una IP distinta por petición
 * (CWE-290).
 */
@Component
public class RateLimitFilter extends OncePerRequestFilter {

    private static final long WINDOW_MILLIS = 60_000;

    private final int maxPerMinute;
    private final List<TrustedProxy> trustedProxies;
    private final Cache<String, Window> windows;

    @Autowired
    public RateLimitFilter(@Value("${app.rate-limit.auth-per-minute:100}") int maxPerMinute,
                           @Value("${app.rate-limit.trusted-proxies:}") String trustedProxies,
                           @Value("${app.rate-limit.cache-max-size:10000}") int cacheMaxSize) {
        this.maxPerMinute = maxPerMinute;
        this.trustedProxies = parseTrustedProxies(trustedProxies);
        this.windows = Caffeine.newBuilder()
                .maximumSize(cacheMaxSize)
                .expireAfterWrite(70, TimeUnit.SECONDS)
                .build();
    }

    /** Compatibilidad con los tests que solo fijan el límite. */
    RateLimitFilter(int maxPerMinute) {
        this(maxPerMinute, "", 10_000);
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

    /**
     * Determina la IP real del cliente.
     *
     * <p>{@code X-Forwarded-For} es una lista {@code cliente, proxy1, proxy2} donde el cliente
     * controla los valores iniciales. Por eso solo se lee si la petición viene de un proxy
     * declarado, y en ese caso se recorre de derecha a izquierda descartando los proxies de
     * confianza: la primera IP que no lo es la añadió el proxy más cercano y es la real. Cualquier
     * valor falsificado por el cliente queda a la izquierda y no se usa.
     */
    private String clientIp(HttpServletRequest request) {
        String remoteAddr = request.getRemoteAddr();
        if (!isTrustedProxy(remoteAddr)) {
            return remoteAddr;
        }
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded == null || forwarded.isBlank()) {
            return remoteAddr;
        }
        String[] hops = forwarded.split(",");
        for (int i = hops.length - 1; i >= 0; i--) {
            String hop = hops[i].trim();
            if (!hop.isEmpty() && !isTrustedProxy(hop)) {
                return hop;
            }
        }
        return remoteAddr;
    }

    private boolean isTrustedProxy(String ip) {
        for (TrustedProxy proxy : trustedProxies) {
            if (proxy.matches(ip)) {
                return true;
            }
        }
        return false;
    }

    private static List<TrustedProxy> parseTrustedProxies(String raw) {
        if (raw == null || raw.isBlank()) {
            return List.of();
        }
        List<TrustedProxy> parsed = new ArrayList<>();
        for (String entry : raw.split(",")) {
            String value = entry.trim();
            if (!value.isEmpty()) {
                parsed.add(TrustedProxy.parse(value));
            }
        }
        return List.copyOf(parsed);
    }

    int cachedWindowCount() {
        return (int) windows.estimatedSize();
    }

    /**
     * Una entrada de {@code app.rate-limit.trusted-proxies}: una IP exacta o un bloque CIDR
     * (IPv4 o IPv6).
     */
    private record TrustedProxy(byte[] address, byte[] network, int prefixBits) {

        private static final String IPV4 = "^\\d{1,3}(\\.\\d{1,3}){3}$";

        static TrustedProxy parse(String value) {
            if (value.contains("/")) {
                String[] parts = value.split("/", 2);
                byte[] network = toAddress(parts[0].trim());
                int prefixBits = Integer.parseInt(parts[1].trim());
                if (prefixBits < 0 || prefixBits > network.length * 8) {
                    throw new IllegalArgumentException("Prefijo CIDR fuera de rango: " + value);
                }
                return new TrustedProxy(null, network, prefixBits);
            }
            return new TrustedProxy(toAddress(value), null, -1);
        }

        /**
         * Convierte una IP literal en sus bytes. Se comprueba primero que es una dirección
         * literal para que {@link InetAddress#getByName} nunca dispare una resolución DNS.
         */
        private static byte[] toAddress(String literal) {
            if (!literal.matches(IPV4) && !literal.contains(":")) {
                throw new IllegalArgumentException("No es una IP literal: " + literal);
            }
            try {
                return InetAddress.getByName(literal).getAddress();
            } catch (UnknownHostException e) {
                throw new IllegalArgumentException("IP no válida: " + literal, e);
            }
        }

        boolean matches(String candidate) {
            byte[] candidateAddress = toAddressOrNull(candidate);
            if (candidateAddress == null) {
                return false;
            }
            if (address != null) {
                return Arrays.equals(address, candidateAddress);
            }
            return sameNetworkPrefix(candidateAddress);
        }

        private boolean sameNetworkPrefix(byte[] candidateAddress) {
            if (candidateAddress.length != network.length) {
                return false;
            }
            int fullBytes = prefixBits / 8;
            int remainderBits = prefixBits % 8;
            for (int i = 0; i < fullBytes; i++) {
                if (candidateAddress[i] != network[i]) {
                    return false;
                }
            }
            if (remainderBits == 0) {
                return true;
            }
            int mask = (0xFF << (8 - remainderBits)) & 0xFF;
            return (candidateAddress[fullBytes] & mask) == (network[fullBytes] & mask);
        }

        private static byte[] toAddressOrNull(String candidate) {
            try {
                return toAddress(candidate);
            } catch (IllegalArgumentException e) {
                return null;
            }
        }
    }

    private static class Window {
        long start = System.currentTimeMillis();
        int count;
    }
}
