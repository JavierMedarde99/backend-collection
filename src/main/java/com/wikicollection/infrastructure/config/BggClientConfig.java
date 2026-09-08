package com.wikicollection.infrastructure.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

@Configuration
public class BggClientConfig {

    @Bean
    public RestTemplate bggJsonRestTemplate() {
        return new RestTemplate();
    }

    @Bean
    public RestTemplate bggXmlRestTemplate() {
        return new RestTemplate();
    }
}