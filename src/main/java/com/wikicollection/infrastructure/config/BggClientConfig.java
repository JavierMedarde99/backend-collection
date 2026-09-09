package com.wikicollection.infrastructure.config;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.web.client.RestTemplate;

@Configuration
public class BggClientConfig {

    @Bean
    public RestTemplate bggXmlRestTemplate(
            @Value("${bgg.auth.token:}") String token,
            RestTemplateBuilder restTemplateBuilder) {
        RestTemplate restTemplate = restTemplateBuilder.build();
        if (token != null && !token.isBlank()) {
            restTemplate.getInterceptors().add(bearerTokenInterceptor(token));
        }
        return restTemplate;
    }

    private ClientHttpRequestInterceptor bearerTokenInterceptor(String token) {
        return (HttpRequest request, byte[] body, ClientHttpRequestExecution execution) -> {
            request.getHeaders().setBearerAuth(token);
            return execution.execute(request, body);
        };
    }
}
