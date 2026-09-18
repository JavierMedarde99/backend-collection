package com.wikicollection.infrastructure.config;

import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    private final String[] allowedOrigins;
    private final CurrentUserHandlerMethodArgumentResolver currentUserResolver;

    public WebConfig(
            @Value("${app.cors.allowed-origins:http://localhost:5173,https://frontend-collection-eta.vercel.app}") String allowedOrigins,
            CurrentUserHandlerMethodArgumentResolver currentUserResolver) {
        this.allowedOrigins = allowedOrigins.split(",");
        this.currentUserResolver = currentUserResolver;
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                .allowedOrigins(allowedOrigins)
                .allowedMethods("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS")
                .allowedHeaders("*");
    }

    @Override
    public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
        resolvers.add(currentUserResolver);
    }
}
