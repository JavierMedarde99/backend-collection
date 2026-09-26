package com.wikicollection.infrastructure.config;

import java.util.List;
import java.util.Map;

import com.github.benmanes.caffeine.cache.Caffeine;

import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableCaching
public class CacheConfig {

    public static final String BOOK_SEARCH = "bookSearch";
    public static final String GAME_SEARCH = "gameSearch";
    public static final String BOARDGAME_SEARCH = "boardgameSearch";
    public static final String MAGIC_SEARCH = "magicSearch";
    public static final String MOVIE_SEARCH = "movieSearch";
    public static final String COMMANDER_SEARCH = "commanderSearch";

    public static final List<String> CACHE_NAMES = List.of(
            BOOK_SEARCH, GAME_SEARCH, BOARDGAME_SEARCH, MAGIC_SEARCH, MOVIE_SEARCH, COMMANDER_SEARCH);

    private static final Map<String, String> PROPERTY_KEYS = Map.of(
            BOOK_SEARCH, "books",
            GAME_SEARCH, "games",
            BOARDGAME_SEARCH, "boardgames",
            MAGIC_SEARCH, "magic",
            MOVIE_SEARCH, "movieshows",
            COMMANDER_SEARCH, "decks");

    private final CacheProperties cacheProperties;

    public CacheConfig(CacheProperties cacheProperties) {
        this.cacheProperties = cacheProperties;
    }

    @Bean
    public CacheManager cacheManager() {
        CaffeineCacheManager cacheManager = new CaffeineCacheManager();
        for (String name : CACHE_NAMES) {
            cacheManager.registerCustomCache(name, Caffeine.newBuilder()
                    .expireAfterWrite(cacheProperties.ttlFor(PROPERTY_KEYS.get(name)))
                    .maximumSize(cacheProperties.getMaxSize())
                    .build());
        }
        cacheManager.setCacheNames(CACHE_NAMES);
        return cacheManager;
    }
}
