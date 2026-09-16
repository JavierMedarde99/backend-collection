package com.wikicollection.infrastructure.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import jakarta.servlet.FilterChain;

import com.wikicollection.application.service.JwtService;
import com.wikicollection.application.service.UserPrincipal;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetailsService;

@ExtendWith(MockitoExtension.class)
class JwtAuthenticationFilterTest {

    @Mock
    private JwtService jwtService;

    @Mock
    private UserDetailsService userDetailsService;

    @Mock
    private FilterChain chain;

    private JwtAuthenticationFilter filter;

    @BeforeEach
    void setUp() {
        filter = new JwtAuthenticationFilter(jwtService, userDetailsService);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private MockHttpServletRequest requestWithToken(String token) {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/books");
        if (token != null) {
            request.addHeader("Authorization", "Bearer " + token);
        }
        return request;
    }

    @Test
    void validToken_setsAuthenticationWithRoleFromClaim() throws Exception {
        UserPrincipal principal = new UserPrincipal("u1", "javi", "hash", "USER");
        when(jwtService.isTokenValid("t")).thenReturn(true);
        when(jwtService.extractUserId("t")).thenReturn("u1");
        when(jwtService.extractRole("t")).thenReturn("USER");
        when(userDetailsService.loadUserByUsername("u1")).thenReturn(principal);

        filter.doFilter(requestWithToken("t"), new MockHttpServletResponse(), chain);

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        assertThat(authentication).isNotNull();
        assertThat(authentication.getPrincipal()).isSameAs(principal);
        assertThat(authentication.getAuthorities()).extracting(Object::toString)
                .containsExactly("ROLE_USER");
        verify(chain).doFilter(any(), any());
    }

    @Test
    void adminToken_setsAdminAuthority() throws Exception {
        UserPrincipal principal = new UserPrincipal("a1", "admin", "hash", "ADMIN");
        when(jwtService.isTokenValid("t")).thenReturn(true);
        when(jwtService.extractUserId("t")).thenReturn("a1");
        when(jwtService.extractRole("t")).thenReturn("ADMIN");
        when(userDetailsService.loadUserByUsername("a1")).thenReturn(principal);

        filter.doFilter(requestWithToken("t"), new MockHttpServletResponse(), chain);

        assertThat(SecurityContextHolder.getContext().getAuthentication().getAuthorities())
                .extracting(Object::toString)
                .containsExactly("ROLE_ADMIN");
    }

    @Test
    void invalidToken_leavesContextEmpty() throws Exception {
        when(jwtService.isTokenValid("bad")).thenReturn(false);

        filter.doFilter(requestWithToken("bad"), new MockHttpServletResponse(), chain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(chain).doFilter(any(), any());
    }

    @Test
    void unknownUser_clearsContext() throws Exception {
        when(jwtService.isTokenValid("t")).thenReturn(true);
        when(jwtService.extractUserId("t")).thenReturn("ghost");
        when(userDetailsService.loadUserByUsername("ghost"))
                .thenThrow(new org.springframework.security.core.userdetails.UsernameNotFoundException("no"));

        filter.doFilter(requestWithToken("t"), new MockHttpServletResponse(), chain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(chain).doFilter(any(), any());
    }

    @Test
    void missingHeader_leavesContextEmpty() throws Exception {
        filter.doFilter(requestWithToken(null), new MockHttpServletResponse(), chain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(chain).doFilter(any(), any());
    }
}
