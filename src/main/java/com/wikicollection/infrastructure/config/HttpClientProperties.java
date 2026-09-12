package com.wikicollection.infrastructure.config;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/**
 * Política HTTP centralizada para todos los clientes externos
 * (timeouts de conexión y lectura). Los reintentos siguen siendo
 * por-cliente (retry-attempts/retry-delay-ms de cada API).
 */
@Component
@ConfigurationProperties(prefix = "http.client")
public class HttpClientProperties {

    private Duration connectTimeout = Duration.ofSeconds(5);
    private Duration readTimeout = Duration.ofSeconds(10);

    public Duration getConnectTimeout() {
        return connectTimeout;
    }

    public void setConnectTimeout(Duration connectTimeout) {
        this.connectTimeout = connectTimeout;
    }

    public Duration getReadTimeout() {
        return readTimeout;
    }

    public void setReadTimeout(Duration readTimeout) {
        this.readTimeout = readTimeout;
    }

    public SimpleClientHttpRequestFactory requestFactory() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(connectTimeout);
        factory.setReadTimeout(readTimeout);
        return factory;
    }

    public RestClient.Builder restClientBuilder(String baseUrl) {
        return RestClient.builder()
                .baseUrl(baseUrl)
                .requestFactory(requestFactory());
    }
}
