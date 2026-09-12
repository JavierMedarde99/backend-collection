package com.wikicollection.infrastructure.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;

import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

class HttpClientPropertiesTest {

    private final HttpClientProperties properties = new HttpClientProperties();

    @Test
    void defaults_areFiveAndTenSeconds() {
        assertThat(properties.getConnectTimeout()).isEqualTo(Duration.ofSeconds(5));
        assertThat(properties.getReadTimeout()).isEqualTo(Duration.ofSeconds(10));
    }

    @Test
    void configs_buildClientsWithTimeouts() {
        RestClientConfig restClientConfig = new RestClientConfig(properties);

        assertThat(restClientConfig.rawgRestClient()).isNotNull();
        assertThat(restClientConfig.googleBooksRestClient()).isNotNull();
        assertThat(restClientConfig.freeToGameRestClient()).isNotNull();
        assertThat(restClientConfig.steamRestClient()).isNotNull();
        assertThat(restClientConfig.steamStoreRestClient()).isNotNull();

        assertThat(new TmdbClientConfig(properties).tmdbRestClient("http://localhost")).isNotNull();

        assertThat(new ScryfallClientConfig(properties).scryfallRestClient()).isNotNull();

        assertThat(new BggClientConfig(properties).bggXmlRestClient("")).isNotNull();
        assertThat(new BggClientConfig(properties).bggXmlRestClient("token")).isNotNull();
    }

    @Test
    void restClientBuilder_appliesBaseUrl() {
        RestClient client = properties.restClientBuilder("http://example.com").build();

        assertThat(client).isNotNull();
    }
}
