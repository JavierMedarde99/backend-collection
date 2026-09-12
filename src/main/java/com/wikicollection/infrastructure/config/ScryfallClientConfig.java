package com.wikicollection.infrastructure.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
public class ScryfallClientConfig {

    @Bean
    public RestClient scryfallRestClient() {
        return RestClient.builder()
                .baseUrl("https://api.scryfall.com")
                .build();
    }
}
