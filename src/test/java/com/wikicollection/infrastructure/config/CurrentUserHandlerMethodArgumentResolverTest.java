package com.wikicollection.infrastructure.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Method;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;

class CurrentUserHandlerMethodArgumentResolverTest {

    private final CurrentUserHandlerMethodArgumentResolver resolver =
            new CurrentUserHandlerMethodArgumentResolver();

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    public void sample(@CurrentUser String userId, String other) {
    }

    private MethodParameter param(int index) throws Exception {
        Method method = getClass().getMethod("sample", String.class, String.class);
        return new MethodParameter(method, index);
    }

    @Test
    void supports_onlyAnnotatedStringParams() throws Exception {
        assertThat(resolver.supportsParameter(param(0))).isTrue();
        assertThat(resolver.supportsParameter(param(1))).isFalse();
    }

    @Test
    void resolve_returnsUsernameFromUserDetails() throws Exception {
        User user = new User("u1", "x", List.of());
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities()));

        assertThat(resolver.resolveArgument(param(0), null, null, null)).isEqualTo("u1");
    }

    @Test
    void resolve_returnsStringPrincipal() throws Exception {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("u9", null, List.of()));

        assertThat(resolver.resolveArgument(param(0), null, null, null)).isEqualTo("u9");
    }

    @Test
    void resolve_returnsNull_whenAnonymous() throws Exception {
        assertThat(resolver.resolveArgument(param(0), null, null, null)).isNull();
    }
}
