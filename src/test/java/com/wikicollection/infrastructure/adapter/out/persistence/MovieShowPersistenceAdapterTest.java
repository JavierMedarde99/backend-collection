package com.wikicollection.infrastructure.adapter.out.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import com.wikicollection.domain.model.MovieMediaType;
import com.wikicollection.domain.model.MovieSearchCriteria;
import com.wikicollection.domain.model.MovieShow;
import com.wikicollection.domain.model.MovieStatus;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;

@ExtendWith(MockitoExtension.class)
class MovieShowPersistenceAdapterTest {

    @Mock
    private SpringDataMovieShowRepository springDataMovieShowRepository;

    @Mock
    private MongoTemplate mongoTemplate;

    @Mock
    private MovieShowEntityMapper mapper;

    @InjectMocks
    private MovieShowPersistenceAdapter adapter;

    private MovieShow sampleShow() {
        return MovieShow.builder()
                .id("m1")
                .externalId("550")
                .title("Fight Club")
                .mediaType(MovieMediaType.MOVIE)
                .status(MovieStatus.WATCHED)
                .externalSource("TMDB")
                .build();
    }

    @Test
    void save_mapsDomainToEntity_andBack() {
        MovieShow show = sampleShow();
        MovieShowEntity entity = MovieShowEntity.builder().id("m1").title("Fight Club").build();
        when(mapper.toEntity(show)).thenReturn(entity);
        when(springDataMovieShowRepository.save(entity)).thenReturn(entity);
        when(mapper.toDomain(entity)).thenReturn(show);

        assertThat(adapter.save(show)).isSameAs(show);
    }

    @Test
    void findById_mapsEntity_whenExists() {
        MovieShowEntity entity = MovieShowEntity.builder().id("m1").title("Fight Club").build();
        MovieShow expected = sampleShow();
        when(springDataMovieShowRepository.findById("m1")).thenReturn(Optional.of(entity));
        when(mapper.toDomain(entity)).thenReturn(expected);

        assertThat(adapter.findById("m1")).contains(expected);
    }

    @Test
    void findById_returnsEmpty_whenMissing() {
        when(springDataMovieShowRepository.findById("nope")).thenReturn(Optional.empty());

        assertThat(adapter.findById("nope")).isEmpty();
    }

    @Test
    void deleteById_delegatesToSpringData() {
        adapter.deleteById("m1");

        verify(springDataMovieShowRepository).deleteById("m1");
    }

    @Test
    void findByExternalId_mapsEntity_whenExists() {
        MovieShowEntity entity = MovieShowEntity.builder().id("m1").externalId("550").build();
        MovieShow expected = sampleShow();
        when(springDataMovieShowRepository.findByExternalId("550")).thenReturn(Optional.of(entity));
        when(mapper.toDomain(entity)).thenReturn(expected);

        assertThat(adapter.findByExternalId("550")).contains(expected);
    }

    @Test
    void count_delegatesToSpringData() {
        when(springDataMovieShowRepository.count()).thenReturn(3L);

        assertThat(adapter.count()).isEqualTo(3L);
    }

    @Test
    void findByCriteria_buildsQueryWithAllFilters() {
        Pageable pageable = PageRequest.of(0, 20);
        when(mongoTemplate.find(any(Query.class), eq(MovieShowEntity.class))).thenReturn(List.of());
        when(mongoTemplate.count(any(Query.class), eq(MovieShowEntity.class))).thenReturn(0L);

        Page<MovieShow> result = adapter.findByCriteria(
                new MovieSearchCriteria("fight", MovieStatus.WATCHED, MovieMediaType.MOVIE), pageable);

        assertThat(result).isEmpty();
        ArgumentCaptor<Query> queryCaptor = ArgumentCaptor.forClass(Query.class);
        verify(mongoTemplate).find(queryCaptor.capture(), eq(MovieShowEntity.class));
        String qs = queryCaptor.getValue().toString();
        assertThat(qs).contains("title");
        assertThat(qs).contains("status");
        assertThat(qs).contains("mediaType");
    }

    @Test
    void findAll_returnsAllResults() {
        Pageable pageable = PageRequest.of(0, 20);
        MovieShowEntity entity = MovieShowEntity.builder().id("m1").title("Fight Club").build();
        MovieShow expected = sampleShow();
        when(mongoTemplate.find(any(Query.class), eq(MovieShowEntity.class))).thenReturn(List.of(entity));
        when(mongoTemplate.count(any(Query.class), eq(MovieShowEntity.class))).thenReturn(1L);
        when(mapper.toDomain(entity)).thenReturn(expected);

        Page<MovieShow> result = adapter.findAll(pageable);

        assertThat(result.getContent()).containsExactly(expected);
    }

    @Test
    void mapper_roundTripsAllFields() {
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

        MovieShow roundTripped = new MovieShowEntityMapper().toDomain(new MovieShowEntityMapper().toEntity(show));

        assertThat(roundTripped).usingRecursiveComparison().isEqualTo(show);
    }

    @Test
    void mapper_handlesNull() {
        MovieShowEntityMapper showMapper = new MovieShowEntityMapper();

        assertThat(showMapper.toEntity(null)).isNull();
        assertThat(showMapper.toDomain(null)).isNull();
    }
}
