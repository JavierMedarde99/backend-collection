package com.wikicollection.infrastructure.config;

import java.time.Duration;
import java.util.List;

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

    static final Duration DEFAULT_TTL = Duration.ofHours(1);
    static final long DEFAULT_MAX_SIZE = 1000;

    @Bean
    public CacheManager cacheManager() {
        CaffeineCacheManager cacheManager = new CaffeineCacheManager();
        cacheManager.setCacheNames(CACHE_NAMES);
        cacheManager.setCaffeine(Caffeine.newBuilder()
                .expireAfterWrite(DEFAULT_TTL)
                .maximumSize(DEFAULT_MAX_SIZE));
        return cacheManager;
    }
}
