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

    public static final String BOOK_DETAIL = "bookDetail";
    public static final String GAME_DETAIL = "gameDetail";
    public static final String BOARDGAME_DETAIL = "boardgameDetail";
    public static final String MOVIE_DETAIL = "movieDetail";
    public static final String MAGIC_DETAIL = "magicDetail";
    public static final String DECK_DETAIL = "deckDetail";
    public static final String USER_PROFILE_DETAIL = "userProfileDetail";

    public static final String BOOK_LIST = "bookList";
    public static final String GAME_LIST = "gameList";
    public static final String BOARDGAME_LIST = "boardgameList";
    public static final String MOVIE_LIST = "movieList";
    public static final String MAGIC_LIST = "magicList";
    public static final String DECK_LIST = "deckList";

    public static final String USER_BOOKS_LIST = "userBooksList";
    public static final String USER_GAMES_LIST = "userGamesList";
    public static final String USER_BOARDGAMES_LIST = "userBoardGamesList";
    public static final String USER_MAGIC_CARDS_LIST = "userMagicCardsList";
    public static final String USER_DECKS_LIST = "userDecksList";
    public static final String USER_MOVIE_SHOWS_LIST = "userMovieShowsList";
    public static final String USER_STREAMS_LIST = "userStreamsList";

    public static final String USER_PREFERENCES_DETAIL = "userPreferencesDetail";
    public static final String USER_ACTIVE_COLLECTIONS = "userActiveCollections";
    public static final String USER_PREFERENCE_FLAGS = "userPreferenceFlags";

    public static final String STATS = "stats";

    public static final List<String> CACHE_NAMES = List.of(
            BOOK_SEARCH, GAME_SEARCH, BOARDGAME_SEARCH, MAGIC_SEARCH, MOVIE_SEARCH, COMMANDER_SEARCH,
            BOOK_DETAIL, GAME_DETAIL, BOARDGAME_DETAIL, MOVIE_DETAIL, MAGIC_DETAIL, DECK_DETAIL, USER_PROFILE_DETAIL,
            BOOK_LIST, GAME_LIST, BOARDGAME_LIST, MOVIE_LIST, MAGIC_LIST, DECK_LIST,
            USER_BOOKS_LIST, USER_GAMES_LIST, USER_BOARDGAMES_LIST, USER_MAGIC_CARDS_LIST, USER_DECKS_LIST,
            USER_MOVIE_SHOWS_LIST, USER_STREAMS_LIST,
            USER_PREFERENCES_DETAIL, USER_ACTIVE_COLLECTIONS, USER_PREFERENCE_FLAGS,
            STATS);

    private static final Map<String, String> PROPERTY_KEYS = Map.ofEntries(
            Map.entry(BOOK_SEARCH, "books"),
            Map.entry(GAME_SEARCH, "games"),
            Map.entry(BOARDGAME_SEARCH, "boardgames"),
            Map.entry(MAGIC_SEARCH, "magic"),
            Map.entry(MOVIE_SEARCH, "movieshows"),
            Map.entry(COMMANDER_SEARCH, "decks"),
            Map.entry(BOOK_DETAIL, "books"),
            Map.entry(GAME_DETAIL, "games"),
            Map.entry(BOARDGAME_DETAIL, "boardgames"),
            Map.entry(MOVIE_DETAIL, "movieshows"),
            Map.entry(MAGIC_DETAIL, "magic"),
            Map.entry(DECK_DETAIL, "decks"),
            Map.entry(USER_PROFILE_DETAIL, "users"),
            Map.entry(BOOK_LIST, "books"),
            Map.entry(GAME_LIST, "games"),
            Map.entry(BOARDGAME_LIST, "boardgames"),
            Map.entry(MOVIE_LIST, "movieshows"),
            Map.entry(MAGIC_LIST, "magic"),
            Map.entry(DECK_LIST, "decks"),
            Map.entry(USER_BOOKS_LIST, "users"),
            Map.entry(USER_GAMES_LIST, "users"),
            Map.entry(USER_BOARDGAMES_LIST, "users"),
            Map.entry(USER_MAGIC_CARDS_LIST, "users"),
            Map.entry(USER_DECKS_LIST, "users"),
            Map.entry(USER_MOVIE_SHOWS_LIST, "users"),
            Map.entry(USER_STREAMS_LIST, "users"),
            Map.entry(USER_PREFERENCES_DETAIL, "preferences"),
            Map.entry(USER_ACTIVE_COLLECTIONS, "preferences"),
            Map.entry(USER_PREFERENCE_FLAGS, "preferences"),
            Map.entry(STATS, "stats"));

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
