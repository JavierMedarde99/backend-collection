package com.wikicollection.infrastructure.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

@Configuration
public class ScryfallClientConfig {

    private final HttpClientProperties httpClientProperties;

    public ScryfallClientConfig(HttpClientProperties httpClientProperties) {
        this.httpClientProperties = httpClientProperties;
    }

    @Bean
    public RestTemplate scryfallRestTemplate() {
        return new RestTemplate(httpClientProperties.requestFactory());
    }
}
