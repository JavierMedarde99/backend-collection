package com.wikicollection.infrastructure.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
public class TmdbClientConfig {

    @Bean
    public RestClient tmdbRestClient(@Value("${tmdb.api.base-url:https://api.themoviedb.org/3}") String baseUrl) {
        return RestClient.builder()
                .baseUrl(baseUrl)
                .build();
    }
}
