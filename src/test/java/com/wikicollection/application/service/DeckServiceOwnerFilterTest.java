package com.wikicollection.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collection;
import java.util.List;

import com.wikicollection.application.exception.UnauthenticatedException;
import com.wikicollection.domain.model.CollectionType;
import com.wikicollection.domain.port.in.UserPreferencesUseCase;
import com.wikicollection.domain.port.out.DeckRepository;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import static org.mockito.Mockito.mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

@ExtendWith(MockitoExtension.class)
class DeckServiceOwnerFilterTest {

    @Mock
    private DeckRepository deckRepository;

    @Mock
    private UserPreferencesUseCase preferencesUseCase;

    @Mock
    private OwnershipValidator ownershipValidator;

    private DeckService service() {
        return new DeckService(deckRepository, null, null, new DeckValidator(),
                ownershipValidator, new OwnerScopeResolver(preferencesUseCase), mock(OwnerResolver.class));
    }

    @Test
    void mine_filtersByViewer() {
        when(deckRepository.findByOwnerId(eq("u1"), any())).thenReturn(Page.empty());

        service().findAll(PageRequest.of(0, 20), "mine", "u1");

        verify(deckRepository).findByOwnerId(eq("u1"), any());
    }

    @Test
    void other_excludesPrivateAndSelf() {
        when(preferencesUseCase.getUserIdsWithPrivateCollection(CollectionType.DECKS))
                .thenReturn(List.of("u9"));
        when(deckRepository.findByOwnerIdNotIn(anyCollection(), any())).thenReturn(Page.empty());

        service().findAll(PageRequest.of(0, 20), "other", "u1");

        verify(deckRepository).findByOwnerIdNotIn(
                argThat(ids -> ids != null && ids.containsAll(List.of("u9", "u1"))), any());
    }

    @Test
    void namedSearch_appliesOwnerScope() {
        when(deckRepository.findByNameAndOwnerId(eq("Mazo"), eq("u1"), any()))
                .thenReturn(Page.empty());

        service().findByName("Mazo", PageRequest.of(0, 20), "mine", "u1");

        verify(deckRepository).findByNameAndOwnerId(eq("Mazo"), eq("u1"), any());
    }

    @Test
    void anonymousMine_throws401() {
        assertThatThrownBy(() -> service().findAll(PageRequest.of(0, 20), "mine", null))
                .isInstanceOf(UnauthenticatedException.class);
    }

    @SuppressWarnings("unchecked")
    private static Collection<String> anyCollection() {
        return any(Collection.class);
    }
}
