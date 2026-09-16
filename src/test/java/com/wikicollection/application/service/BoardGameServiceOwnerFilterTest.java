package com.wikicollection.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import com.wikicollection.application.exception.UnauthenticatedException;
import com.wikicollection.domain.model.CollectionType;
import com.wikicollection.domain.model.BoardGame;
import com.wikicollection.domain.model.BoardGameSearchCriteria;
import com.wikicollection.domain.port.in.UserPreferencesUseCase;
import com.wikicollection.domain.port.out.BoardGameRepository;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import static org.mockito.Mockito.mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

@ExtendWith(MockitoExtension.class)
class BoardGameServiceOwnerFilterTest {

    @Mock
    private BoardGameRepository boardGameRepository;

    @Mock
    private UserPreferencesUseCase preferencesUseCase;

    private BoardGameService service() {
        OwnerScopeResolver resolver = new OwnerScopeResolver(preferencesUseCase);
        return new BoardGameService(boardGameRepository, mock(OwnershipValidator.class), new OwnerScopeResolver(preferencesUseCase));
    }

    @Test
    void mine_filtersByViewer() {
        when(boardGameRepository.search(any(BoardGameSearchCriteria.class), any())).thenReturn(Page.empty());
        var pageable = PageRequest.of(0, 20);

        service().search(new BoardGameSearchCriteria(null, null, null, null), pageable, "mine", "u1");

        ArgumentCaptor<BoardGameSearchCriteria> captor = ArgumentCaptor.forClass(BoardGameSearchCriteria.class);
        verify(boardGameRepository).search(captor.capture(), any());
        assertThat(captor.getValue().ownerId()).isEqualTo("u1");
    }

    @Test
    void other_excludesPrivate() {
        when(preferencesUseCase.getUserIdsWithPrivateCollection(CollectionType.BOARDGAMES))
                .thenReturn(List.of("u9"));
        when(boardGameRepository.search(any(BoardGameSearchCriteria.class), any())).thenReturn(Page.empty());

        service().search(new BoardGameSearchCriteria(null, null, null, null), PageRequest.of(0, 20), "other", null);

        ArgumentCaptor<BoardGameSearchCriteria> captor = ArgumentCaptor.forClass(BoardGameSearchCriteria.class);
        verify(boardGameRepository).search(captor.capture(), any());
        assertThat(captor.getValue().excludeOwnerIds()).containsExactly("u9");
    }

    @Test
    void anonymousMine_throws401() {
        assertThatThrownBy(() -> service().search(new BoardGameSearchCriteria(null, null, null, null), PageRequest.of(0, 20), "mine", null))
                .isInstanceOf(UnauthenticatedException.class);
    }

    @Test
    void unknownMode_throws400() {
        assertThatThrownBy(() -> service().search(new BoardGameSearchCriteria(null, null, null, null), PageRequest.of(0, 20), "bogus", "u1"))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
