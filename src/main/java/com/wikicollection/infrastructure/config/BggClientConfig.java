package com.wikicollection.infrastructure.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.web.client.RestTemplate;

@Configuration
public class BggClientConfig {

    @Bean
    public RestTemplate bggXmlRestTemplate(@Value("${bgg.auth.token:}") String token) {
        RestTemplate restTemplate = new RestTemplate();
        if (token != null && !token.isBlank()) {
            restTemplate.getInterceptors().add(bearerTokenInterceptor(token));
        }
        return restTemplate;
    }

    private ClientHttpRequestInterceptor bearerTokenInterceptor(String token) {
        return (request, body, execution) -> {
            request.getHeaders().setBearerAuth(token);
            return execution.execute(request, body);
        };
    }
}
