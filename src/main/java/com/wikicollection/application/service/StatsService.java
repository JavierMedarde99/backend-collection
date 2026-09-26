package com.wikicollection.application.service;

import java.util.LinkedHashMap;
import java.util.Map;

import com.wikicollection.domain.model.BoardGameSearchCriteria;
import com.wikicollection.domain.model.BookSearchCriteria;
import com.wikicollection.domain.model.CollectionType;
import com.wikicollection.domain.model.GameSearchCriteria;
import com.wikicollection.domain.model.MagicCardSearchCriteria;
import com.wikicollection.domain.model.MovieSearchCriteria;
import com.wikicollection.domain.port.in.StatsUseCase;
import com.wikicollection.domain.port.out.BoardGameRepository;
import com.wikicollection.domain.port.out.BookRepository;
import com.wikicollection.domain.port.out.DeckRepository;
import com.wikicollection.domain.port.out.GameRepository;
import com.wikicollection.domain.port.out.MagicCardRepository;
import com.wikicollection.domain.port.out.MovieShowRepository;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

@Service
public class StatsService implements StatsUseCase {

    private final BookRepository bookRepository;
    private final GameRepository gameRepository;
    private final BoardGameRepository boardGameRepository;
    private final MagicCardRepository magicCardRepository;
    private final DeckRepository deckRepository;
    private final MovieShowRepository movieShowRepository;

    public StatsService(BookRepository bookRepository,
                        GameRepository gameRepository,
                        BoardGameRepository boardGameRepository,
                        MagicCardRepository magicCardRepository,
                        DeckRepository deckRepository,
                        MovieShowRepository movieShowRepository) {
        this.bookRepository = bookRepository;
        this.gameRepository = gameRepository;
        this.boardGameRepository = boardGameRepository;
        this.magicCardRepository = magicCardRepository;
        this.deckRepository = deckRepository;
        this.movieShowRepository = movieShowRepository;
    }

    @Override
    @Cacheable(cacheNames = "stats", key = "'global'")
    public Map<String, Long> getGlobalCounts() {
        Pageable single = PageRequest.of(0, 1);
        Map<String, Long> counts = new LinkedHashMap<>();
        counts.put(CollectionType.BOOKS.getKey(), bookRepository
                .search(new BookSearchCriteria(null, null, null, null, null, null, null), single).getTotalElements());
        counts.put(CollectionType.GAMES.getKey(), gameRepository
                .search(new GameSearchCriteria(null, null, null, null, null, null), single).getTotalElements());
        counts.put(CollectionType.BOARDGAMES.getKey(), boardGameRepository
                .search(new BoardGameSearchCriteria(null, null, null, null, null), single).getTotalElements());
        counts.put(CollectionType.MAGIC.getKey(), magicCardRepository
                .search(new MagicCardSearchCriteria(null, null, null, null, null, null), single).getTotalElements());
        counts.put(CollectionType.DECKS.getKey(), deckRepository.findAll(single).getTotalElements());
        counts.put(CollectionType.MOVIESHOWS.getKey(), movieShowRepository
                .findByCriteria(new MovieSearchCriteria(null, null, null, null, null, null), single).getTotalElements());
        return counts;
    }
}
