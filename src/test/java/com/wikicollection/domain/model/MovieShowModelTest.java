package com.wikicollection.domain.model;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;

class MovieShowModelTest {

    @Test
    void movieShow_buildsWithAllFields() {
        MovieShow show = MovieShow.builder()
                .id("m1")
                .externalId("550")
                .title("Fight Club")
                .overview("Un oficinista...")
                .releaseDate(LocalDate.of(1999, 10, 15))
                .posterUrl("http://poster")
                .backdropUrl("http://backdrop")
                .voteAverage(8.4)
                .mediaType(MovieMediaType.MOVIE)
                .status(MovieStatus.WATCHED)
                .userRating(5)
                .comment("Obra maestra")
                .dateAdded(LocalDate.of(2026, 1, 1))
                .dateCompleted(LocalDate.of(2026, 1, 2))
                .externalSource("TMDB")
                .build();

        assertThat(show.getId()).isEqualTo("m1");
        assertThat(show.getExternalId()).isEqualTo("550");
        assertThat(show.getTitle()).isEqualTo("Fight Club");
        assertThat(show.getReleaseDate()).isEqualTo(LocalDate.of(1999, 10, 15));
        assertThat(show.getMediaType()).isEqualTo(MovieMediaType.MOVIE);
        assertThat(show.getStatus()).isEqualTo(MovieStatus.WATCHED);
        assertThat(show.getUserRating()).isEqualTo(5);
        assertThat(show.getExternalSource()).isEqualTo("TMDB");
    }

    @Test
    void enums_haveExpectedValues() {
        assertThat(MovieStatus.values()).containsExactly(
                MovieStatus.WATCHING, MovieStatus.WATCHED, MovieStatus.WISHLIST, MovieStatus.PLAN_TO_WATCH);
        assertThat(MovieMediaType.values()).containsExactly(MovieMediaType.MOVIE, MovieMediaType.TV);
    }
}
