package com.wikicollection.infrastructure.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

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
}
