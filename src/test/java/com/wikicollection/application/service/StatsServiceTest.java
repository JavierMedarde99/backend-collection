package com.wikicollection.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;

import com.wikicollection.domain.port.out.BoardGameRepository;
import com.wikicollection.domain.port.out.BookRepository;
import com.wikicollection.domain.port.out.DeckRepository;
import com.wikicollection.domain.port.out.GameRepository;
import com.wikicollection.domain.port.out.MagicCardRepository;
import com.wikicollection.domain.port.out.MovieShowRepository;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

@ExtendWith(MockitoExtension.class)
class StatsServiceTest {

    @Mock
    private BookRepository bookRepository;

    @Mock
    private GameRepository gameRepository;

    @Mock
    private BoardGameRepository boardGameRepository;

    @Mock
    private MagicCardRepository magicCardRepository;

    @Mock
    private DeckRepository deckRepository;

    @Mock
    private MovieShowRepository movieShowRepository;

    @InjectMocks
    private StatsService service;

    @Test
    void globalCounts_countsEveryCollection() {
        var pageable = PageRequest.of(0, 1);
        when(bookRepository.search(any(), any()))
                .thenReturn(new PageImpl<>(List.of(), pageable, 10));
        when(gameRepository.search(any(), any()))
                .thenReturn(new PageImpl<>(List.of(), pageable, 5));
        when(boardGameRepository.search(any(), any()))
                .thenReturn(new PageImpl<>(List.of(), pageable, 3));
        when(magicCardRepository.search(any(), any()))
                .thenReturn(new PageImpl<>(List.of(), pageable, 7));
        when(deckRepository.findAll(any()))
                .thenReturn(new PageImpl<>(List.of(), pageable, 2));
        when(movieShowRepository.findByCriteria(any(), any()))
                .thenReturn(new PageImpl<>(List.of(), pageable, 4));

        Map<String, Long> counts = service.getGlobalCounts();

        assertThat(counts).containsExactlyInAnyOrderEntriesOf(Map.of(
                "books", 10L, "games", 5L, "boardgames", 3L,
                "magic", 7L, "decks", 2L, "movieshows", 4L));
    }
}
