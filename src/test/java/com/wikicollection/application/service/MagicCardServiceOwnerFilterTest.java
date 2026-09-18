package com.wikicollection.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import com.wikicollection.application.exception.UnauthenticatedException;
import com.wikicollection.domain.model.CollectionType;
import com.wikicollection.domain.model.MagicCard;
import com.wikicollection.domain.model.MagicCardSearchCriteria;
import com.wikicollection.domain.port.in.UserPreferencesUseCase;
import com.wikicollection.domain.port.out.MagicCardRepository;

import com.wikicollection.domain.port.out.ExternalMagicCardCatalogClient;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import static org.mockito.Mockito.mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

@ExtendWith(MockitoExtension.class)
class MagicCardServiceOwnerFilterTest {

    @Mock
    private MagicCardRepository magicCardRepository;

    @Mock
    private UserPreferencesUseCase preferencesUseCase;

    private MagicCardService service() {
        OwnerScopeResolver resolver = new OwnerScopeResolver(preferencesUseCase);
        return new MagicCardService(magicCardRepository, mock(ExternalMagicCardCatalogClient.class), mock(OwnershipValidator.class), new OwnerScopeResolver(preferencesUseCase), mock(OwnerResolver.class));
    }

    @Test
    void mine_filtersByViewer() {
        when(magicCardRepository.search(any(MagicCardSearchCriteria.class), any())).thenReturn(Page.empty());
        var pageable = PageRequest.of(0, 20);

        service().search(new MagicCardSearchCriteria(null, null, null, null, null, null), pageable, "mine", "u1");

        ArgumentCaptor<MagicCardSearchCriteria> captor = ArgumentCaptor.forClass(MagicCardSearchCriteria.class);
        verify(magicCardRepository).search(captor.capture(), any());
        assertThat(captor.getValue().ownerId()).isEqualTo("u1");
    }

    @Test
    void other_excludesPrivate() {
        when(preferencesUseCase.getUserIdsWithPrivateCollection(CollectionType.MAGIC))
                .thenReturn(List.of("u9"));
        when(magicCardRepository.search(any(MagicCardSearchCriteria.class), any())).thenReturn(Page.empty());

        service().search(new MagicCardSearchCriteria(null, null, null, null, null, null), PageRequest.of(0, 20), "other", null);

        ArgumentCaptor<MagicCardSearchCriteria> captor = ArgumentCaptor.forClass(MagicCardSearchCriteria.class);
        verify(magicCardRepository).search(captor.capture(), any());
        assertThat(captor.getValue().excludeOwnerIds()).containsExactly("u9");
    }

    @Test
    void anonymousMine_throws401() {
        assertThatThrownBy(() -> service().search(new MagicCardSearchCriteria(null, null, null, null, null, null), PageRequest.of(0, 20), "mine", null))
                .isInstanceOf(UnauthenticatedException.class);
    }

    @Test
    void unknownMode_throws400() {
        assertThatThrownBy(() -> service().search(new MagicCardSearchCriteria(null, null, null, null, null, null), PageRequest.of(0, 20), "bogus", "u1"))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
