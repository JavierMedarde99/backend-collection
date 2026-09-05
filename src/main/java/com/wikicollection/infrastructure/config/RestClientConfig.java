package com.wikicollection.infrastructure.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
public class RestClientConfig {

    @Bean
    public RestClient googleBooksRestClient() {
        return RestClient.builder()
                .baseUrl("https://www.googleapis.com/books")
                .build();
    }

    @Bean
    public RestClient rawgRestClient() {
        return RestClient.builder()
                .baseUrl("https://api.rawg.io/api")
                .build();
    }

    @Bean
    public RestClient freeToGameRestClient() {
        return RestClient.builder()
                .baseUrl("https://www.freetogame.com/api")
                .build();
    }
}