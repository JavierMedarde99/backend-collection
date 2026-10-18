package com.wikicollection.infrastructure.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.util.concurrent.TimeUnit;

import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class RateLimitFilterTest {

    private MockHttpServletRequest authRequest(String ip) {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/auth/login");
        request.setRemoteAddr(ip);
        return request;
    }

    private MockHttpServletRequest authRequestWithForwardedFor(String ip, String forwardedFor) {
        MockHttpServletRequest request = authRequest(ip);
        request.addHeader("X-Forwarded-For", forwardedFor);
        return request;
    }

    @Test
    void allowsRequests_underLimit() throws Exception {
        RateLimitFilter filter = new RateLimitFilter(2);
        FilterChain chain = mock(FilterChain.class);

        filter.doFilter(authRequest("1.1.1.1"), new MockHttpServletResponse(), chain);
        filter.doFilter(authRequest("1.1.1.1"), new MockHttpServletResponse(), chain);

        verify(chain, times(2)).doFilter(
                org.mockito.ArgumentMatchers.any(HttpServletRequest.class),
                org.mockito.ArgumentMatchers.any(HttpServletResponse.class));
    }

    @Test
    void rejectsRequests_overLimit_with429() throws Exception {
        RateLimitFilter filter = new RateLimitFilter(2);
        FilterChain chain = mock(FilterChain.class);

        filter.doFilter(authRequest("2.2.2.2"), new MockHttpServletResponse(), chain);
        filter.doFilter(authRequest("2.2.2.2"), new MockHttpServletResponse(), chain);
        MockHttpServletResponse blocked = new MockHttpServletResponse();
        filter.doFilter(authRequest("2.2.2.2"), blocked, chain);

        assertThat(blocked.getStatus()).isEqualTo(429);
        verify(chain, times(2)).doFilter(
                org.mockito.ArgumentMatchers.any(HttpServletRequest.class),
                org.mockito.ArgumentMatchers.any(HttpServletResponse.class));
    }

    @Test
    void countsLimits_perIp() throws Exception {
        RateLimitFilter filter = new RateLimitFilter(1);
        FilterChain chain = mock(FilterChain.class);

        filter.doFilter(authRequest("3.3.3.3"), new MockHttpServletResponse(), chain);
        MockHttpServletResponse other = new MockHttpServletResponse();
        filter.doFilter(authRequest("4.4.4.4"), other, chain);

        assertThat(other.getStatus()).isEqualTo(200);
    }

    @Test
    void ignoresNonAuthPaths() throws Exception {
        RateLimitFilter filter = new RateLimitFilter(1);
        FilterChain chain = mock(FilterChain.class);
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/books");
        request.setRemoteAddr("5.5.5.5");

        filter.doFilter(request, new MockHttpServletResponse(), chain);
        filter.doFilter(request, new MockHttpServletResponse(), chain);

        verify(chain, times(2)).doFilter(
                org.mockito.ArgumentMatchers.any(HttpServletRequest.class),
                org.mockito.ArgumentMatchers.any(HttpServletResponse.class));
    }

    @Test
    void spoofedForwardedHeader_cannotBypassLimit_withoutTrustedProxy() throws Exception {
        RateLimitFilter filter = new RateLimitFilter(2);
        FilterChain chain = mock(FilterChain.class);

        filter.doFilter(authRequestWithForwardedFor("9.9.9.9", "1.1.1.1"), new MockHttpServletResponse(), chain);
        filter.doFilter(authRequestWithForwardedFor("9.9.9.9", "2.2.2.2"), new MockHttpServletResponse(), chain);
        MockHttpServletResponse blocked = new MockHttpServletResponse();
        filter.doFilter(authRequestWithForwardedFor("9.9.9.9", "3.3.3.3"), blocked, chain);

        assertThat(blocked.getStatus())
                .as("Sin proxy de confianza, X-Forwarded-For no debe contar para nada: la clave debe ser la IP real")
                .isEqualTo(429);
    }

    @Test
    void forwardedHeader_isIgnored_whenRemoteAddrIsNotAProxy() throws Exception {
        RateLimitFilter filter = new RateLimitFilter(2, "10.0.0.1", 1000);
        FilterChain chain = mock(FilterChain.class);

        filter.doFilter(authRequestWithForwardedFor("8.8.8.8", "1.1.1.1"), new MockHttpServletResponse(), chain);
        filter.doFilter(authRequestWithForwardedFor("8.8.8.8", "2.2.2.2"), new MockHttpServletResponse(), chain);
        MockHttpServletResponse blocked = new MockHttpServletResponse();
        filter.doFilter(authRequestWithForwardedFor("8.8.8.8", "3.3.3.3"), blocked, chain);

        assertThat(blocked.getStatus())
                .as("Solo el proxy de confianza puede dictaminar la IP del cliente")
                .isEqualTo(429);
    }

    @Test
    void forwardedHeader_isTrusted_whenRemoteAddrIsAProxy() throws Exception {
        RateLimitFilter filter = new RateLimitFilter(2, "10.0.0.1", 1000);
        FilterChain chain = mock(FilterChain.class);

        filter.doFilter(authRequestWithForwardedFor("10.0.0.1", "7.7.7.7"), new MockHttpServletResponse(), chain);
        MockHttpServletResponse second = new MockHttpServletResponse();
        filter.doFilter(authRequestWithForwardedFor("10.0.0.1", "8.8.8.8"), second, chain);

        assertThat(second.getStatus())
                .as("Detrás del proxy, cada IP de cliente real debe tener su propia ventana")
                .isEqualTo(200);
        verify(chain, times(2)).doFilter(
                org.mockito.ArgumentMatchers.any(HttpServletRequest.class),
                org.mockito.ArgumentMatchers.any(HttpServletResponse.class));
    }

    @Test
    void rightmostUntrustedIp_isTheClient_behindTrustedProxy() throws Exception {
        RateLimitFilter filter = new RateLimitFilter(2, "10.0.0.1", 1000);
        FilterChain chain = mock(FilterChain.class);

        // El cliente falsifica el primer valor; el proxy añade la IP real al final.
        filter.doFilter(authRequestWithForwardedFor("10.0.0.1", "1.1.1.1, 5.5.5.5"), new MockHttpServletResponse(), chain);
        filter.doFilter(authRequestWithForwardedFor("10.0.0.1", "2.2.2.2, 5.5.5.5"), new MockHttpServletResponse(), chain);
        MockHttpServletResponse blocked = new MockHttpServletResponse();
        filter.doFilter(authRequestWithForwardedFor("10.0.0.1", "3.3.3.3, 5.5.5.5"), blocked, chain);

        assertThat(blocked.getStatus())
                .as("Debe contar la IP que añadió el proxy (la última no-confianza), no la falsificada")
                .isEqualTo(429);
    }

    @Test
    void trustedProxies_acceptCidrBlocks() throws Exception {
        RateLimitFilter filter = new RateLimitFilter(1, "10.0.0.0/8", 1000);
        FilterChain chain = mock(FilterChain.class);

        // Mismo cliente real (último hop), valores falsificados delante.
        filter.doFilter(authRequestWithForwardedFor("10.99.0.7", "1.1.1.1, 4.4.4.4"), new MockHttpServletResponse(), chain);
        MockHttpServletResponse blocked = new MockHttpServletResponse();
        filter.doFilter(authRequestWithForwardedFor("10.99.0.7", "2.2.2.2, 4.4.4.4"), blocked, chain);

        assertThat(blocked.getStatus())
                .as("Un proxy identificado por rango CIDR también debe ser de confianza")
                .isEqualTo(429);
    }

    @Test
    void windowsCache_isBounded() throws InterruptedException {
        RateLimitFilter filter = new RateLimitFilter(100, "", 10);
        FilterChain chain = mock(FilterChain.class);

        for (int i = 0; i < 200; i++) {
            try {
                filter.doFilter(authRequest("10.0." + (i / 256) + "." + (i % 256)), new MockHttpServletResponse(), chain);
            } catch (Exception e) {
                throw new IllegalStateException(e);
            }
        }

        // Caffeine expulsa de forma asíncrona (ForkJoinPool común), así que cuando la JVM
        // está saturada con el resto de la suite la tarea de mantenimiento llega tarde y el
        // tamaño sigue por encima del tope justo al terminar el bucle. Esperamos a que
        // termine antes de comprobar: sin esto el test era flaky (#503).
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
        while (filter.cachedWindowCount() > 20 && System.nanoTime() < deadline) {
            Thread.sleep(10);
        }

        assertThat(filter.cachedWindowCount())
                .as("La caché de ventanas no debe crecer sin límite con IPs arbitrarias. Caffeine "
                        + "evicta de forma asíncrona, así que el tamaño puede superar el máximo "
                        + "transitoriamente; lo que importa es que quede acotado y no crezca con "
                        + "las %d IPs insertadas", 200)
                .isLessThanOrEqualTo(20);
    }
}
