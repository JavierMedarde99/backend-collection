package com.wikicollection.infrastructure.adapter.out.bgg.json;

import java.util.List;

import com.wikicollection.domain.model.BoardGameSearchResult;
import com.wikicollection.domain.port.out.ExternalBoardGameCatalogClient;
import com.wikicollection.infrastructure.adapter.out.bgg.mapper.BoardGameJsonMapper;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

@Slf4j
@Component("bggJsonClient")
public class BggJsonClient implements ExternalBoardGameCatalogClient {

    private static final String SEARCH_PATH = "/search";

    private final RestTemplate bggJsonRestTemplate;
    private final String baseUrl;
    private final BoardGameJsonMapper mapper;

    public BggJsonClient(@Qualifier("bggJsonRestTemplate") RestTemplate bggJsonRestTemplate,
                         @Value("${bgg.api.base-url:https://bgg.cc/api/v1}") String baseUrl,
                         BoardGameJsonMapper mapper) {
        this.bggJsonRestTemplate = bggJsonRestTemplate;
        this.baseUrl = baseUrl;
        this.mapper = mapper;
    }

    @Override
    public List<BoardGameSearchResult> search(String query) {
        try {
            String uri = UriComponentsBuilder.fromUriString(baseUrl)
                    .path(SEARCH_PATH)
                    .queryParam("query", query)
                    .build()
                    .toUriString();
            BoardGameJsonMapper.BggJsonGame[] games =
                    bggJsonRestTemplate.getForObject(uri, BoardGameJsonMapper.BggJsonGame[].class);
            return mapper.map(games);
        } catch (RestClientResponseException e) {
            log.warn("BGG JSON devolvió error {}: {}", e.getStatusCode(), e.getResponseBodyAsString());
            return List.of();
        } catch (ResourceAccessException e) {
            log.warn("BGG JSON no disponible: {}", e.getMessage());
            return List.of();
        }
    }
}