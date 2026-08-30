package com.wikicollection.config;

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
}