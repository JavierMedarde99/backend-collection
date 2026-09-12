package com.wikicollection.infrastructure.config;

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

    @Test
    void addCorsMappings_usesConfiguredOrigins() {
        when(registry.addMapping("/api/**")).thenReturn(registration);
        when(registration.allowedOrigins(any(String[].class))).thenReturn(registration);
        when(registration.allowedMethods(any(String[].class))).thenReturn(registration);

        new WebConfig("https://app.example.com,https://admin.example.com").addCorsMappings(registry);

        verify(registry).addMapping("/api/**");
        verify(registration).allowedOrigins("https://app.example.com", "https://admin.example.com");
    }
}
