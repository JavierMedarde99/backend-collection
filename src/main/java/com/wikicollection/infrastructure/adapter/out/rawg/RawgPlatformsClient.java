package com.wikicollection.infrastructure.adapter.out.rawg;

import java.util.List;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import com.wikicollection.domain.model.PlatformInfo;
import com.wikicollection.domain.port.out.PlatformCatalogClient;
import com.wikicollection.infrastructure.config.CacheConfig;

import lombok.extern.slf4j.Slf4j;

/**
 * Catálogo de plataformas de RAWG. El {@code RestClient} ya tiene como base
 * {@code https://api.rawg.io/api}, de modo que la ruta es {@code /platforms}.
 */
@Slf4j
@Component
public class RawgPlatformsClient implements PlatformCatalogClient {

    private static final String PLATFORMS_PATH = "/platforms";

    private final RestClient rawgRestClient;
    private final String apiKey;

    public RawgPlatformsClient(
            @Qualifier("rawgRestClient") RestClient rawgRestClient,
            @Value("${rawg.api-key:}") String apiKey) {
        this.rawgRestClient = rawgRestClient;
        this.apiKey = apiKey;
    }

    @Cacheable(cacheNames = CacheConfig.PLATFORM_SEARCH, key = "'all'", unless = "#result.isEmpty()")
    @Override
    public List<PlatformInfo> getPlatforms() {
        try {
            PlatformsResponse response = rawgRestClient.get()
                    .uri(uriBuilder -> {
                        uriBuilder.path(PLATFORMS_PATH);
                        if (apiKey != null && !apiKey.isBlank()) {
                            uriBuilder.queryParam("key", apiKey);
                        }
                        return uriBuilder.build();
                    })
                    .retrieve()
                    .body(PlatformsResponse.class);

            if (response == null || response.results() == null) {
                return List.of();
            }
            return response.results().stream()
                    .filter(platform -> platform != null && platform.name() != null && !platform.name().isBlank())
                    .map(platform -> new PlatformInfo(
                            platform.id() != null ? platform.id().longValue() : null,
                            platform.name(),
                            platform.slug()))
                    .toList();
        } catch (RestClientResponseException e) {
            log.warn("RAWG devolvió error {} al listar plataformas: {}", e.getStatusCode(), e.getResponseBodyAsString());
            return List.of();
        } catch (ResourceAccessException e) {
            log.warn("RAWG no disponible al listar plataformas: {}", e.getMessage());
            return List.of();
        }
    }

    record PlatformsResponse(List<RawgPlatform> results) {
    }

    record RawgPlatform(Integer id, String name, String slug) {
    }
}