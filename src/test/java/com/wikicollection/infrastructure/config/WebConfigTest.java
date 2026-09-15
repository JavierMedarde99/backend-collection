package com.wikicollection.infrastructure.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.servlet.config.annotation.CorsRegistration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;

@ExtendWith(MockitoExtension.class)
class WebConfigTest {

    @Mock
    private CorsRegistry registry;

    @Mock
    private CorsRegistration registration;

    @Mock
    private CurrentUserHandlerMethodArgumentResolver currentUserResolver;

    @Test
    void addCorsMappings_usesConfiguredOrigins() {
        when(registry.addMapping("/api/**")).thenReturn(registration);
        when(registration.allowedOrigins(any(String[].class))).thenReturn(registration);
        when(registration.allowedMethods(any(String[].class))).thenReturn(registration);

        new WebConfig("https://app.example.com,https://admin.example.com", currentUserResolver)
                .addCorsMappings(registry);

        verify(registry).addMapping("/api/**");
        verify(registration).allowedOrigins("https://app.example.com", "https://admin.example.com");
    }

    @Test
    void addArgumentResolvers_registersCurrentUserResolver() {
        java.util.List<org.springframework.web.method.support.HandlerMethodArgumentResolver> resolvers =
                new java.util.ArrayList<>();

        new WebConfig("https://app.example.com", currentUserResolver).addArgumentResolvers(resolvers);

        assertThat(resolvers).containsExactly(currentUserResolver);
    }
}
