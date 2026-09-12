package com.wikicollection.infrastructure.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.web.client.RestClient;

@Configuration
public class BggClientConfig {

    @Bean
    public RestClient bggXmlRestClient(@Value("${bgg.auth.token:}") String token) {
        var builder = RestClient.builder()
                .baseUrl("https://boardgamegeek.com/xmlapi2");
        if (token != null && !token.isBlank()) {
            builder.requestInterceptor(bearerTokenInterceptor(token));
        }
        return builder.build();
    }

    private ClientHttpRequestInterceptor bearerTokenInterceptor(String token) {
        return (request, body, execution) -> {
            request.getHeaders().setBearerAuth(token);
            return execution.execute(request, body);
        };
    }
}
