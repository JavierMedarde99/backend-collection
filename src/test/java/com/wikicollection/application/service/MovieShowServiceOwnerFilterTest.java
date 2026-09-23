package com.wikicollection.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import com.wikicollection.application.exception.UnauthenticatedException;
import com.wikicollection.domain.model.CollectionType;
import com.wikicollection.domain.model.MovieShow;
import com.wikicollection.domain.model.MovieSearchCriteria;
import com.wikicollection.domain.port.in.UserPreferencesUseCase;
import com.wikicollection.domain.port.out.MovieShowRepository;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import static org.mockito.Mockito.mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

@ExtendWith(MockitoExtension.class)
class MovieShowServiceOwnerFilterTest {

    @Mock
    private MovieShowRepository movieShowRepository;

    @Mock
    private UserPreferencesUseCase preferencesUseCase;

    private MovieShowService service() {
        OwnerScopeResolver resolver = new OwnerScopeResolver(preferencesUseCase);
        return new MovieShowService(movieShowRepository, mock(DateRangeValidator.class), mock(OwnershipValidator.class), new OwnerScopeResolver(preferencesUseCase), mock(OwnerResolver.class),
                mock(com.wikicollection.domain.port.out.WatchProvidersClient.class),
                mock(com.wikicollection.infrastructure.adapter.out.tmdb.ProviderUrlMapper.class));
    }

    @Test
    void mine_filtersByViewer() {
        when(movieShowRepository.findByCriteria(any(MovieSearchCriteria.class), any())).thenReturn(Page.empty());
        var pageable = PageRequest.of(0, 20);

        service().search(new MovieSearchCriteria(null, null, null, null, null), pageable, "mine", "u1");

        ArgumentCaptor<MovieSearchCriteria> captor = ArgumentCaptor.forClass(MovieSearchCriteria.class);
        verify(movieShowRepository).findByCriteria(captor.capture(), any());
        assertThat(captor.getValue().ownerId()).isEqualTo("u1");
    }

    @Test
    void other_excludesPrivate() {
        when(preferencesUseCase.getUserIdsWithPrivateCollection(CollectionType.MOVIESHOWS))
                .thenReturn(List.of("u9"));
        when(movieShowRepository.findByCriteria(any(MovieSearchCriteria.class), any())).thenReturn(Page.empty());

        service().search(new MovieSearchCriteria(null, null, null, null, null), PageRequest.of(0, 20), "other", null);

        ArgumentCaptor<MovieSearchCriteria> captor = ArgumentCaptor.forClass(MovieSearchCriteria.class);
        verify(movieShowRepository).findByCriteria(captor.capture(), any());
        assertThat(captor.getValue().excludeOwnerIds()).containsExactly("u9");
    }

    @Test
    void anonymousMine_throws401() {
        assertThatThrownBy(() -> service().search(new MovieSearchCriteria(null, null, null, null, null), PageRequest.of(0, 20), "mine", null))
                .isInstanceOf(UnauthenticatedException.class);
    }

    @Test
    void unknownMode_throws400() {
        assertThatThrownBy(() -> service().search(new MovieSearchCriteria(null, null, null, null, null), PageRequest.of(0, 20), "bogus", "u1"))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
