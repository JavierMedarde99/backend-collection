package com.wikicollection.infrastructure.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

@Configuration
public class ScryfallClientConfig {

    @Bean
    public RestTemplate scryfallRestTemplate() {
        return new RestTemplate();
    }
}
