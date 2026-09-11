package com.wikicollection.infrastructure.adapter.in.web.dto;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;

import com.wikicollection.domain.model.MovieMediaType;
import com.wikicollection.domain.model.MovieShow;
import com.wikicollection.domain.model.MovieStatus;

import org.junit.jupiter.api.Test;

class MovieShowDtoMapperTest {

    private final MovieShowDtoMapper mapper = new MovieShowDtoMapper();

    @Test
    void toDomain_mapsRequestFields() {
        MovieShowRequest request = new MovieShowRequest("550", "Fight Club", "Overview",
                LocalDate.of(1999, 10, 15), "http://poster", "http://backdrop", 8.4,
                MovieMediaType.MOVIE, MovieStatus.WATCHED, 5, "Nota",
                LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 2), "TMDB");

        MovieShow show = mapper.toDomain(request);

        assertThat(show.getExternalId()).isEqualTo("550");
        assertThat(show.getTitle()).isEqualTo("Fight Club");
        assertThat(show.getReleaseDate()).isEqualTo(LocalDate.of(1999, 10, 15));
        assertThat(show.getMediaType()).isEqualTo(MovieMediaType.MOVIE);
        assertThat(show.getStatus()).isEqualTo(MovieStatus.WATCHED);
        assertThat(show.getUserRating()).isEqualTo(5);
        assertThat(show.getExternalSource()).isEqualTo("TMDB");
    }

    @Test
    void toResponse_mapsDomainFields() {
        MovieShow show = MovieShow.builder()
                .id("m1")
                .externalId("550")
                .title("Fight Club")
                .overview("Overview")
                .releaseDate(LocalDate.of(1999, 10, 15))
                .posterUrl("http://poster")
                .backdropUrl("http://backdrop")
                .voteAverage(8.4)
                .mediaType(MovieMediaType.MOVIE)
                .status(MovieStatus.WATCHED)
                .userRating(5)
                .comment("Nota")
                .dateAdded(LocalDate.of(2026, 1, 1))
                .dateCompleted(LocalDate.of(2026, 1, 2))
                .externalSource("TMDB")
                .build();

        MovieShowResponse response = mapper.toResponse(show);

        assertThat(response.id()).isEqualTo("m1");
        assertThat(response.title()).isEqualTo("Fight Club");
        assertThat(response.mediaType()).isEqualTo(MovieMediaType.MOVIE);
        assertThat(response.status()).isEqualTo(MovieStatus.WATCHED);
        assertThat(response.dateCompleted()).isEqualTo(LocalDate.of(2026, 1, 2));
    }

    @Test
    void mappers_returnNull_whenNull() {
        assertThat(mapper.toDomain(null)).isNull();
        assertThat(mapper.toResponse(null)).isNull();
    }
}
