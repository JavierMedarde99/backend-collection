package com.wikicollection.application.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.stream.Collectors;
import java.util.stream.IntStream;

import com.wikicollection.domain.port.in.MagicCardSearchUseCase;
import com.wikicollection.domain.port.in.MovieSearchUseCase;
import com.wikicollection.infrastructure.config.CacheConfig;

import okhttp3.mockwebserver.Dispatcher;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.CacheManager;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

@SpringBootTest(properties = {
        "spring.data.mongodb.auto-index-creation=false",
        "app.boardgame-status-migration.enabled=false",
        "app.cache.ttl.magic=1s",
        "app.cache.ttl.movieshows=1s"})
class CachedPagedSearchTest {

    private static MockWebServer server;

    @Autowired
    private MagicCardSearchUseCase magicSearch;
    @Autowired
    private MovieSearchUseCase movieSearch;
    @Autowired
    private CacheManager cacheManager;

    @BeforeAll
    static void startServer() throws Exception {
        server = new MockWebServer();
        server.setDispatcher(new Dispatcher() {
            @Override
            public MockResponse dispatch(RecordedRequest request) {
                String path = request.getPath();
                if (path.startsWith("/cards/search")) {
                    return jsonResponse(scryfallList(5));
                }
                if (path.startsWith("/search/movie")) {
                    return jsonResponse(tmdbList("movie"));
                }
                if (path.startsWith("/search/tv")) {
                    return jsonResponse(tmdbList("tv"));
                }
                return new MockResponse().setResponseCode(404);
            }
        });
        server.start();
    }

    @AfterAll
    static void stopServer() throws Exception {
        server.shutdown();
    }

    @DynamicPropertySource
    static void backendUrls(DynamicPropertyRegistry registry) {
        registry.add("scryfall.api.base-url", () -> server.url("").toString());
        registry.add("tmdb.api.base-url", () -> server.url("").toString());
    }

    @BeforeEach
    void clearCaches() {
        CacheConfig.CACHE_NAMES.forEach(name -> cacheManager.getCache(name).clear());
    }

    @Test
    void pagesShareSingleUpstreamRequest() {
        int before = server.getRequestCount();

        var first = magicSearch.search("bolt", PageRequest.of(0, 2));
        var second = magicSearch.search("bolt", PageRequest.of(1, 2));
        var third = magicSearch.search("bolt", PageRequest.of(2, 2));

        assertThat(first.getContent()).hasSize(2);
        assertThat(first.getTotalElements()).isEqualTo(5);
        assertThat(first.getTotalPages()).isEqualTo(3);
        assertThat(second.getContent()).hasSize(2);
        assertThat(third.getContent()).hasSize(1);
        assertThat(server.getRequestCount() - before).isEqualTo(1);
    }

    @Test
    void expiredTtlRefetches() throws Exception {
        int before = server.getRequestCount();

        magicSearch.search("bolt", PageRequest.of(0, 10));
        Thread.sleep(1200);
        var afterExpiry = magicSearch.search("bolt", PageRequest.of(0, 10));

        assertThat(afterExpiry.getTotalElements()).isEqualTo(5);
        assertThat(server.getRequestCount() - before).isEqualTo(2);
    }

    @Test
    void clearedCacheRefetches() {
        int before = server.getRequestCount();

        magicSearch.search("bolt", PageRequest.of(0, 10));
        cacheManager.getCache(CacheConfig.MAGIC_SEARCH).clear();
        magicSearch.search("bolt", PageRequest.of(0, 10));

        assertThat(server.getRequestCount() - before).isEqualTo(2);
    }

    @Test
    void tmdbCachesBothEndpointsPerQuery() {
        int before = server.getRequestCount();

        var first = movieSearch.search("matrix", null, PageRequest.of(0, 1));
        var second = movieSearch.search("matrix", null, PageRequest.of(1, 1));

        assertThat(first.getTotalElements()).isEqualTo(2);
        assertThat(first.getContent()).hasSize(1);
        assertThat(second.getContent()).hasSize(1);
        assertThat(server.getRequestCount() - before).isEqualTo(2);
    }

    private static MockResponse jsonResponse(String body) {
        return new MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                .setBody(body);
    }

    private static String scryfallList(int size) {
        String data = IntStream.range(0, size)
                .mapToObj(i -> """
                        {"id":"sf-%d","name":"Carta %d","mana_cost":"{R}","type_line":"Instant",
                         "rarity":"common","set":"x","set_name":"X","colors":["R"],
                         "color_identity":["R"],"prices":{"usd":"0.1"},
                         "image_uris":{"normal":"http://img"}}""".formatted(i, i))
                .collect(Collectors.joining(","));
        return "{\"object\":\"list\",\"data\":[" + data + "]}";
    }

    private static String tmdbList(String kind) {
        String title = kind.equals("movie") ? "Matrix" : "Breaking Bad";
        String date = kind.equals("movie") ? "release_date" : "first_air_date";
        String nameField = kind.equals("movie") ? "\"title\":\"" + title + "\"" : "\"name\":\"" + title + "\"";
        return """
                {"page":1,"results":[
                  {"id":1,%s,"overview":"...","%s":"1999-10-15",
                   "poster_path":"/p.jpg","vote_average":8.0}
                ],"total_results":1}""".formatted(nameField, date);
    }
}
