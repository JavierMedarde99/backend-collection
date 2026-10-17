package com.wikicollection.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import com.wikicollection.application.exception.UserNotFoundException;
import com.wikicollection.domain.model.BookSearchCriteria;
import com.wikicollection.domain.model.CollectionVisibility;
import com.wikicollection.domain.model.User;
import com.wikicollection.domain.model.UserPreferences;
import com.wikicollection.domain.port.out.BoardGameRepository;
import com.wikicollection.domain.port.out.BookRepository;
import com.wikicollection.domain.port.out.DeckRepository;
import com.wikicollection.domain.port.out.GameRepository;
import com.wikicollection.domain.port.out.MagicCardRepository;
import com.wikicollection.domain.port.out.MovieShowRepository;
import com.wikicollection.domain.port.out.UserPreferencesRepository;
import com.wikicollection.domain.port.out.UserProfilePort;
import com.wikicollection.domain.model.AmiiboSearchCriteria;
import com.wikicollection.domain.model.FunkoSearchCriteria;
import com.wikicollection.domain.port.out.AmiiboRepository;
import com.wikicollection.domain.port.out.FunkoRepository;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

@ExtendWith(MockitoExtension.class)
class UserProfileServiceTest {

    @Mock
    private UserProfilePort userProfilePort;

    @Mock
    private UserPreferencesRepository preferencesRepository;

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

    @Mock
    private AmiiboRepository amiiboRepository;

    @Mock
    private FunkoRepository funkoRepository;

    @InjectMocks
    private UserProfileService service;

    private User sampleUser() {
        return User.builder().id("u1").username("javi").build();
    }

    @Test
    void getPublicProfile_returnsUser_whenExists() {
        when(userProfilePort.findByUsername("javi")).thenReturn(Optional.of(sampleUser()));

        assertThat(service.getPublicProfile("javi").getId()).isEqualTo("u1");
    }

    @Test
    void getPublicProfile_throwsNotFound_whenMissing() {
        when(userProfilePort.findByUsername("ghost")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getPublicProfile("ghost"))
                .isInstanceOf(UserNotFoundException.class);
    }

    @Test
    void getPublicBooks_searchesByOwner_whenPublic() {
        when(userProfilePort.findByUsername("javi")).thenReturn(Optional.of(sampleUser()));
        when(preferencesRepository.findByUserId("u1"))
                .thenReturn(Optional.of(UserPreferences.defaults("u1")));
        when(bookRepository.search(any(BookSearchCriteria.class), any()))
                .thenReturn(Page.empty());
        var pageable = PageRequest.of(0, 20);

        service.getPublicBooks("javi", pageable);

        ArgumentCaptor<BookSearchCriteria> captor = ArgumentCaptor.forClass(BookSearchCriteria.class);
        verify(bookRepository).search(captor.capture(), eq(pageable));
        assertThat(captor.getValue().ownerId()).isEqualTo("u1");
    }

    @Test
    void getPublicBooks_returnsEmpty_whenPrivate() {
        when(userProfilePort.findByUsername("javi")).thenReturn(Optional.of(sampleUser()));
        UserPreferences prefs = UserPreferences.defaults("u1");
        prefs.getCollectionVisibility().put("books", CollectionVisibility.PRIVATE);
        when(preferencesRepository.findByUserId("u1")).thenReturn(Optional.of(prefs));

        assertThat(service.getPublicBooks("javi", PageRequest.of(0, 20))).isEmpty();
    }

    @Test
    void getPublicBooks_treatsMissingPreferencesAsPublic() {
        when(userProfilePort.findByUsername("javi")).thenReturn(Optional.of(sampleUser()));
        when(preferencesRepository.findByUserId("u1")).thenReturn(Optional.empty());
        when(bookRepository.search(any(BookSearchCriteria.class), any()))
                .thenReturn(Page.empty());

        service.getPublicBooks("javi", PageRequest.of(0, 20));

        verify(bookRepository).search(any(BookSearchCriteria.class), any());
    }

    @Test
    void getPublicAmiibos_returnsEmpty_whenPrivate() {
        when(userProfilePort.findByUsername("javi")).thenReturn(Optional.of(sampleUser()));
        UserPreferences prefs = UserPreferences.defaults("u1");
        prefs.getCollectionVisibility().put("amiibos", CollectionVisibility.PRIVATE);
        when(preferencesRepository.findByUserId("u1")).thenReturn(Optional.of(prefs));

        assertThat(service.getPublicAmiibos("javi", PageRequest.of(0, 20))).isEmpty();
    }

    @Test
    void getPublicAmiibos_searches_whenPublic() {
        when(userProfilePort.findByUsername("javi")).thenReturn(Optional.of(sampleUser()));
        UserPreferences prefs = UserPreferences.defaults("u1");
        prefs.getCollectionVisibility().put("amiibos", CollectionVisibility.PUBLIC);
        when(preferencesRepository.findByUserId("u1")).thenReturn(Optional.of(prefs));
        when(amiiboRepository.search(any(AmiiboSearchCriteria.class), any())).thenReturn(Page.empty());

        service.getPublicAmiibos("javi", PageRequest.of(0, 20));

        ArgumentCaptor<AmiiboSearchCriteria> captor = ArgumentCaptor.forClass(AmiiboSearchCriteria.class);
        verify(amiiboRepository).search(captor.capture(), any());
        assertThat(captor.getValue().ownerId()).isEqualTo("u1");
    }

    @Test
    void getPublicFunkos_returnsEmpty_whenPrivate() {
        when(userProfilePort.findByUsername("javi")).thenReturn(Optional.of(sampleUser()));
        UserPreferences prefs = UserPreferences.defaults("u1");
        prefs.getCollectionVisibility().put("funkos", CollectionVisibility.PRIVATE);
        when(preferencesRepository.findByUserId("u1")).thenReturn(Optional.of(prefs));

        assertThat(service.getPublicFunkos("javi", PageRequest.of(0, 20))).isEmpty();
    }

    @Test
    void getPublicFunkos_searches_whenPublic() {
        when(userProfilePort.findByUsername("javi")).thenReturn(Optional.of(sampleUser()));
        UserPreferences prefs = UserPreferences.defaults("u1");
        prefs.getCollectionVisibility().put("funkos", CollectionVisibility.PUBLIC);
        when(preferencesRepository.findByUserId("u1")).thenReturn(Optional.of(prefs));
        when(funkoRepository.search(any(FunkoSearchCriteria.class), any())).thenReturn(Page.empty());

        service.getPublicFunkos("javi", PageRequest.of(0, 20));

        ArgumentCaptor<FunkoSearchCriteria> captor = ArgumentCaptor.forClass(FunkoSearchCriteria.class);
        verify(funkoRepository).search(captor.capture(), any());
        assertThat(captor.getValue().ownerId()).isEqualTo("u1");
    }
}
