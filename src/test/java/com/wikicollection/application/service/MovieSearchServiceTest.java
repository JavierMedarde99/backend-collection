package com.wikicollection.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.List;

import com.wikicollection.domain.model.MovieMediaType;
import com.wikicollection.domain.model.MovieSearchResult;
import com.wikicollection.domain.port.out.ExternalMovieCatalogClient;

import org.junit.jupiter.api.BeforeEach;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

class MovieSearchServiceTest {

    @Mock
    private ExternalMovieCatalogClient catalogClient;

    private MovieSearchService movieSearchService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        movieSearchService = new MovieSearchService(catalogClient);
    }

    private MovieSearchResult sampleResult(String title) {
        return new MovieSearchResult("550", title, "Overview", LocalDate.of(1999, 10, 15),
                "http://poster", "http://backdrop", 8.4, MovieMediaType.MOVIE, "TMDB");
    }

    @Test
    void search_returnsResultsFromCatalog() {
        MovieSearchResult result = sampleResult("Fight Club");
        when(catalogClient.search("fight", null)).thenReturn(List.of(result));

        Page<MovieSearchResult> results = movieSearchService.search("fight", PageRequest.of(0, 10));

        assertThat(results.getContent()).containsExactly(result);
        verify(catalogClient).search("fight", null);
    }

    @Test
    void search_withMediaType_delegatesFilter() {
        MovieSearchResult result = sampleResult("Fight Club");
        when(catalogClient.search("fight", MovieMediaType.MOVIE)).thenReturn(List.of(result));

        Page<MovieSearchResult> results = movieSearchService.search("fight", MovieMediaType.MOVIE, PageRequest.of(0, 10));

        assertThat(results.getContent()).containsExactly(result);
        verify(catalogClient).search("fight", MovieMediaType.MOVIE);
    }

    @Test
    void search_limitsResultsToTen() {
        List<MovieSearchResult> results = java.util.stream.IntStream.range(0, 15)
                .mapToObj(i -> sampleResult("Título " + i))
                .toList();
        when(catalogClient.search("titulo", null)).thenReturn(results);

        Page<MovieSearchResult> first = movieSearchService.search("titulo", PageRequest.of(0, 10));
        Page<MovieSearchResult> second = movieSearchService.search("titulo", null, PageRequest.of(1, 10));

        assertThat(first.getContent()).isEqualTo(results.subList(0, 10));
        assertThat(first.getTotalElements()).isEqualTo(15);
        assertThat(second.getContent()).isEqualTo(results.subList(10, 15));
    }

    @Test
    void search_rejectsBlankQuery() {
        assertThatThrownBy(() -> movieSearchService.search("  ", PageRequest.of(0, 10)))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
