package com.wikicollection.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import com.wikicollection.domain.model.BoardGameSearchResult;
import com.wikicollection.domain.model.BookSearchResult;
import com.wikicollection.domain.model.GamePlatform;
import com.wikicollection.domain.model.GameSearchResult;
import com.wikicollection.domain.model.MagicCardSearchResult;
import com.wikicollection.domain.model.MovieMediaType;
import com.wikicollection.domain.model.MovieSearchResult;
import com.wikicollection.domain.port.in.BoardGameSearchUseCase;
import com.wikicollection.domain.port.in.BookSearchUseCase;
import com.wikicollection.domain.port.in.DeckSearchUseCase;
import com.wikicollection.domain.port.in.GameSearchUseCase;
import com.wikicollection.domain.port.in.MagicCardSearchUseCase;
import com.wikicollection.domain.port.in.MovieSearchUseCase;
import com.wikicollection.domain.port.out.ExternalBoardGameCatalogClient;
import com.wikicollection.domain.port.out.ExternalBookCatalogClient;
import com.wikicollection.domain.port.out.ExternalGameCatalogClient;
import com.wikicollection.domain.port.out.ExternalMagicCardCatalogClient;
import com.wikicollection.domain.port.out.ExternalMovieCatalogClient;
import com.wikicollection.infrastructure.config.CacheConfig;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.CacheManager;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@SpringBootTest(properties = {
        "spring.data.mongodb.auto-index-creation=false",
        "app.boardgame-status-migration.enabled=false",
        "app.cache.ttl.books=1s",
        "app.cache.ttl.games=1s",
        "app.cache.ttl.boardgames=1s",
        "app.cache.ttl.magic=1s",
        "app.cache.ttl.movieshows=1s",
        "app.cache.ttl.decks=1s"})
class SearchCacheTest {

    @Autowired
    private BookSearchUseCase bookSearch;
    @Autowired
    private GameSearchUseCase gameSearch;
    @Autowired
    private BoardGameSearchUseCase boardGameSearch;
    @Autowired
    private MagicCardSearchUseCase magicSearch;
    @Autowired
    private MovieSearchUseCase movieSearch;
    @Autowired
    private DeckSearchUseCase deckSearch;
    @Autowired
    private CacheManager cacheManager;

    @MockitoBean
    private ExternalBookCatalogClient bookCatalog;
    @MockitoBean(name = "rawgClient")
    private ExternalGameCatalogClient gameCatalog;
    @MockitoBean(name = "freeToGameClient")
    private ExternalGameCatalogClient freeToGameCatalog;
    @MockitoBean
    private ExternalBoardGameCatalogClient boardGameCatalog;
    @MockitoBean
    private ExternalMagicCardCatalogClient magicCatalog;
    @MockitoBean
    private ExternalMovieCatalogClient movieCatalog;

    @BeforeEach
    void clearCaches() {
        CacheConfig.CACHE_NAMES.forEach(name -> cacheManager.getCache(name).clear());
    }

    @Test
    void repeatedSearches_hitCache() {
        when(bookCatalog.search("dune")).thenReturn(List.of(bookResult()));
        when(gameCatalog.search("witcher")).thenReturn(List.of(gameResult()));
        when(boardGameCatalog.search("catan")).thenReturn(List.of(boardGameResult()));
        when(magicCatalog.search("bolt")).thenReturn(List.of(magicResult()));
        when(magicCatalog.searchCommanders("rug")).thenReturn(List.of(magicResult()));
        when(movieCatalog.search("matrix", null)).thenReturn(List.of(movieResult()));

        assertThat(bookSearch.search("dune")).hasSize(1);
        assertThat(bookSearch.search("dune")).hasSize(1);
        assertThat(gameSearch.search("witcher")).hasSize(1);
        assertThat(gameSearch.search("witcher")).hasSize(1);
        assertThat(boardGameSearch.search("catan")).hasSize(1);
        assertThat(boardGameSearch.search("catan")).hasSize(1);
        assertThat(magicSearch.search("bolt")).hasSize(1);
        assertThat(magicSearch.search("bolt")).hasSize(1);
        assertThat(deckSearch.searchCommanders("rug")).hasSize(1);
        assertThat(deckSearch.searchCommanders("rug")).hasSize(1);
        assertThat(movieSearch.search("matrix", null)).hasSize(1);
        assertThat(movieSearch.search("matrix", null)).hasSize(1);

        verify(bookCatalog, times(1)).search("dune");
        verify(gameCatalog, times(1)).search("witcher");
        verify(boardGameCatalog, times(1)).search("catan");
        verify(magicCatalog, times(1)).search("bolt");
        verify(magicCatalog, times(1)).searchCommanders("rug");
        verify(movieCatalog, times(1)).search("matrix", null);
    }

    @Test
    void differentQueries_useDifferentKeys() {
        when(bookCatalog.search("dune")).thenReturn(List.of(bookResult()));
        when(bookCatalog.search("it")).thenReturn(List.of(bookResult()));
        when(movieCatalog.search("matrix", MovieMediaType.MOVIE)).thenReturn(List.of(movieResult()));
        when(movieCatalog.search("matrix", MovieMediaType.TV)).thenReturn(List.of(movieResult()));

        bookSearch.search("dune");
        bookSearch.search("it");
        movieSearch.search("matrix", MovieMediaType.MOVIE);
        movieSearch.search("matrix", MovieMediaType.TV);

        verify(bookCatalog, times(1)).search("dune");
        verify(bookCatalog, times(1)).search("it");
        verify(movieCatalog, times(1)).search("matrix", MovieMediaType.MOVIE);
        verify(movieCatalog, times(1)).search("matrix", MovieMediaType.TV);
    }

    @Test
    void expiredTtl_triggersNewRequest() throws Exception {
        when(bookCatalog.search("dune")).thenReturn(List.of(bookResult()));

        bookSearch.search("dune");
        Thread.sleep(1200);
        bookSearch.search("dune");

        verify(bookCatalog, times(2)).search("dune");
    }

    @Test
    void clearedCache_triggersNewRequest() {
        when(gameCatalog.search("witcher")).thenReturn(List.of(gameResult()));

        gameSearch.search("witcher");
        cacheManager.getCache(CacheConfig.GAME_SEARCH).clear();
        gameSearch.search("witcher");

        verify(gameCatalog, times(2)).search("witcher");
    }

    @Test
    void errors_areNotCached() {
        when(boardGameCatalog.search("catan"))
                .thenThrow(new RuntimeException("boom"))
                .thenReturn(List.of(boardGameResult()));

        try {
            boardGameSearch.search("catan");
        } catch (RuntimeException expected) {
        }
        assertThat(boardGameSearch.search("catan")).hasSize(1);

        verify(boardGameCatalog, times(2)).search("catan");
    }

    private BookSearchResult bookResult() {
        return new BookSearchResult("1", "Dune", List.of("Herbert"), "isbn",
                null, null, null, null, null, null, null);
    }

    private GameSearchResult gameResult() {
        return new GameSearchResult("1", "Witcher", null, null, GamePlatform.PC,
                null, null, null, null, "RAWG");
    }

    private BoardGameSearchResult boardGameResult() {
        return new BoardGameSearchResult("1", "Catan", null, null, null, null,
                null, null, null, null, null, null, null, null, BigDecimal.ONE, "BGG");
    }

    private MagicCardSearchResult magicResult() {
        return new MagicCardSearchResult("id-1", "Bolt", "{R}", "Instant", "common",
                "set", "Set", null, null, List.of("R"), List.of("R"), "texto");
    }

    private MovieSearchResult movieResult() {
        return new MovieSearchResult("1", "Matrix", null, LocalDate.of(1999, 3, 31),
                null, null, 8.0, MovieMediaType.MOVIE, "TMDB");
    }
}
