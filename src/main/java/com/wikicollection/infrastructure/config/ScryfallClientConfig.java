package com.wikicollection.infrastructure.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
public class ScryfallClientConfig {

    private final HttpClientProperties httpClientProperties;

    public ScryfallClientConfig(HttpClientProperties httpClientProperties) {
        this.httpClientProperties = httpClientProperties;
    }

    @Bean
    public RestClient scryfallRestClient() {
        return httpClientProperties.restClientBuilder("https://api.scryfall.com").build();
    }
}
