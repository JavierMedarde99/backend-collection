package com.wikicollection.infrastructure.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
public class RestClientConfig {

    private final HttpClientProperties httpClientProperties;

    public RestClientConfig(HttpClientProperties httpClientProperties) {
        this.httpClientProperties = httpClientProperties;
    }

    @Bean
    public RestClient googleBooksRestClient() {
        return httpClientProperties.restClientBuilder("https://www.googleapis.com/books").build();
    }

    @Bean
    public RestClient rawgRestClient() {
        return httpClientProperties.restClientBuilder("https://api.rawg.io/api").build();
    }

    @Bean
    public RestClient freeToGameRestClient() {
        return httpClientProperties.restClientBuilder("https://www.freetogame.com/api").build();
    }

    @Bean
    public RestClient steamRestClient() {
        return httpClientProperties.restClientBuilder("https://api.steampowered.com").build();
    }

    @Bean
    public RestClient steamStoreRestClient() {
        return httpClientProperties.restClientBuilder("https://store.steampowered.com/api").build();
    }
}
