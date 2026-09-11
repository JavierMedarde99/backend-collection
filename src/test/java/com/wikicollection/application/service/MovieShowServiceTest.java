package com.wikicollection.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.Optional;

import com.wikicollection.application.exception.MovieShowConflictException;
import com.wikicollection.application.exception.MovieShowNotFoundException;
import com.wikicollection.domain.model.MovieMediaType;
import com.wikicollection.domain.model.MovieSearchCriteria;
import com.wikicollection.domain.model.MovieShow;
import com.wikicollection.domain.model.MovieStatus;
import com.wikicollection.domain.port.out.MovieShowRepository;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

@ExtendWith(MockitoExtension.class)
class MovieShowServiceTest {

    @Mock
    private MovieShowRepository movieShowRepository;

    @Spy
    private DateRangeValidator dateRangeValidator = new DateRangeValidator();

    @InjectMocks
    private MovieShowService movieShowService;

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
    void search_delegatesToRepository() {
        Pageable pageable = PageRequest.of(0, 20);
        MovieSearchCriteria criteria = new MovieSearchCriteria("fight", MovieStatus.WATCHED, null);
        when(movieShowRepository.findByCriteria(criteria, pageable)).thenReturn(Page.empty());

        assertThat(movieShowService.search(criteria, pageable)).isEmpty();
        verify(movieShowRepository).findByCriteria(criteria, pageable);
    }

    @Test
    void findById_returnsShow_whenExists() {
        MovieShow show = sampleShow();
        when(movieShowRepository.findById("m1")).thenReturn(Optional.of(show));

        assertThat(movieShowService.findById("m1")).isSameAs(show);
    }

    @Test
    void findById_throwsNotFound_whenMissing() {
        when(movieShowRepository.findById("nope")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> movieShowService.findById("nope"))
                .isInstanceOf(MovieShowNotFoundException.class)
                .hasMessageContaining("nope");
    }

    @Test
    void save_persists_whenNoDuplicate() {
        MovieShow show = sampleShow();
        show.setId(null);
        when(movieShowRepository.findByExternalId("550")).thenReturn(Optional.empty());
        when(movieShowRepository.save(any(MovieShow.class))).thenAnswer(invocation -> invocation.getArgument(0));

        MovieShow result = movieShowService.save(show);

        assertThat(result).isSameAs(show);
    }

    @Test
    void save_throwsConflict_whenDuplicateExternalId() {
        MovieShow show = sampleShow();
        show.setId(null);
        when(movieShowRepository.findByExternalId("550")).thenReturn(Optional.of(sampleShow()));

        assertThatThrownBy(() -> movieShowService.save(show))
                .isInstanceOf(MovieShowConflictException.class)
                .hasMessageContaining("550");
    }

    @Test
    void save_skipsDuplicateCheck_whenBlankExternalId() {
        MovieShow show = sampleShow();
        show.setId(null);
        show.setExternalId("  ");
        when(movieShowRepository.save(any(MovieShow.class))).thenAnswer(invocation -> invocation.getArgument(0));

        MovieShow result = movieShowService.save(show);

        assertThat(result).isSameAs(show);
        verify(movieShowRepository, org.mockito.Mockito.never()).findByExternalId(any());
    }

    @Test
    void save_rejectsFutureDates() {
        MovieShow show = sampleShow();
        show.setDateAdded(LocalDate.now().plusDays(1));

        assertThatThrownBy(() -> movieShowService.save(show))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void update_copiesFields() {
        MovieShow existing = sampleShow();
        MovieShow updates = sampleShow();
        updates.setTitle("Se7en");
        updates.setUserRating(5);
        when(movieShowRepository.findById("m1")).thenReturn(Optional.of(existing));
        when(movieShowRepository.findByExternalId("550")).thenReturn(Optional.of(existing));
        when(movieShowRepository.save(any(MovieShow.class))).thenAnswer(invocation -> invocation.getArgument(0));

        MovieShow result = movieShowService.update("m1", updates);

        assertThat(result.getId()).isEqualTo("m1");
        assertThat(result.getTitle()).isEqualTo("Se7en");
        assertThat(result.getUserRating()).isEqualTo(5);
    }

    @Test
    void update_throwsNotFound_whenMissing() {
        when(movieShowRepository.findById("nope")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> movieShowService.update("nope", sampleShow()))
                .isInstanceOf(MovieShowNotFoundException.class);
    }

    @Test
    void delete_deletesShow_whenExists() {
        when(movieShowRepository.findById("m1")).thenReturn(Optional.of(sampleShow()));

        movieShowService.delete("m1");

        verify(movieShowRepository).deleteById("m1");
    }

    @Test
    void delete_throwsNotFound_whenMissing() {
        when(movieShowRepository.findById("nope")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> movieShowService.delete("nope"))
                .isInstanceOf(MovieShowNotFoundException.class);
    }
}
