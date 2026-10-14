package com.wikicollection.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import com.wikicollection.application.exception.AmiiboConflictException;
import com.wikicollection.application.exception.AmiiboNotFoundException;
import com.wikicollection.application.exception.ForbiddenException;
import com.wikicollection.domain.model.Amiibo;
import com.wikicollection.domain.model.AmiiboReleaseDates;
import com.wikicollection.domain.model.AmiiboSearchCriteria;
import com.wikicollection.domain.model.AmiiboStatus;
import com.wikicollection.domain.model.AmiiboType;
import com.wikicollection.domain.model.CollectionType;
import com.wikicollection.domain.model.UserOwned;
import com.wikicollection.domain.port.out.AmiiboRepository;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

@ExtendWith(MockitoExtension.class)
class AmiiboServiceTest {

    @Mock
    private AmiiboRepository amiiboRepository;

    @Mock
    private OwnershipValidator ownershipValidator;

    @Mock
    private OwnerScopeResolver ownerScopeResolver;

    @Mock
    private OwnerResolver ownerResolver;

    @InjectMocks
    private AmiiboService service;

    private Amiibo amiibo() {
        return Amiibo.builder()
                .externalId("09d0030102bb0e02")
                .name("Metal Mario")
                .status(AmiiboStatus.OWNED)
                .build();
    }

    @Test
    void save_setsOwnerAndUserName_andPersists() {
        Amiibo input = amiibo();
        UserOwned owned = UserOwned.builder().ownerId("u1").ownerName("Javi").username("javi").build();
        when(amiiboRepository.findByExternalId("09d0030102bb0e02")).thenReturn(Optional.empty());
        when(ownerResolver.resolveOwner("u1")).thenReturn(owned);
        when(amiiboRepository.save(any(Amiibo.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Amiibo saved = service.save(input, "u1");

        assertThat(saved.getOwnerId()).isEqualTo("u1");
        assertThat(saved.getUserOwned()).isEqualTo(owned);
        verify(amiiboRepository).save(input);
    }

    @Test
    void save_throwsConflict_whenExternalIdAlreadyExists() {
        Amiibo input = amiibo();
        Amiibo existing = Amiibo.builder().id("am-other").externalId("09d0030102bb0e02").build();
        when(amiiboRepository.findByExternalId("09d0030102bb0e02")).thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> service.save(input, "u1"))
                .isInstanceOf(AmiiboConflictException.class);
        verify(amiiboRepository, never()).save(any(Amiibo.class));
    }

    @Test
    void save_throwsConflict_onDuplicateKey() {
        Amiibo input = amiibo();
        when(amiiboRepository.findByExternalId("09d0030102bb0e02")).thenReturn(Optional.empty());
        when(amiiboRepository.save(any(Amiibo.class))).thenThrow(new DuplicateKeyException("dup"));

        assertThatThrownBy(() -> service.save(input, "u1"))
                .isInstanceOf(AmiiboConflictException.class);
    }

    @Test
    void update_copiesAllUpdatableFieldsAndPersists() {
        Amiibo existing = Amiibo.builder()
                .id("am1")
                .ownerId("u1")
                .externalId("old")
                .name("Old")
                .status(AmiiboStatus.WISHLIST)
                .build();
        Amiibo updates = Amiibo.builder()
                .externalId("09d0030102bb0e02")
                .name("Metal Mario")
                .character("Metal Mario")
                .amiiboSeries("Mario Sports Superstars")
                .gameSeries("Mario Sports Superstars")
                .amiiboType(AmiiboType.FIGURE)
                .releaseDates(new AmiiboReleaseDates(LocalDate.of(2017, 6, 16), null, null, null))
                .imageUrl("https://img")
                .compatibleGames(List.of("Mario Sports Superstars"))
                .status(AmiiboStatus.OWNED)
                .notes("nuevo")
                .dateAdded(LocalDate.of(2026, 10, 13))
                .acquisitionDate(LocalDate.of(2024, 5, 1))
                .acquisitionPrice(new BigDecimal("14.50"))
                .build();
        when(amiiboRepository.findById("am1")).thenReturn(Optional.of(existing));
        when(amiiboRepository.findByExternalId("09d0030102bb0e02")).thenReturn(Optional.empty());
        when(amiiboRepository.save(any(Amiibo.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Amiibo saved = service.update("am1", updates, "u1");

        verify(ownershipValidator).validateOwner("u1", "u1");
        assertThat(saved.getId()).isEqualTo("am1");
        assertThat(saved.getOwnerId()).isEqualTo("u1");
        assertThat(saved.getExternalId()).isEqualTo("09d0030102bb0e02");
        assertThat(saved.getName()).isEqualTo("Metal Mario");
        assertThat(saved.getCharacter()).isEqualTo("Metal Mario");
        assertThat(saved.getAmiiboSeries()).isEqualTo("Mario Sports Superstars");
        assertThat(saved.getGameSeries()).isEqualTo("Mario Sports Superstars");
        assertThat(saved.getAmiiboType()).isEqualTo(AmiiboType.FIGURE);
        assertThat(saved.getReleaseDates())
                .isEqualTo(new AmiiboReleaseDates(LocalDate.of(2017, 6, 16), null, null, null));
        assertThat(saved.getImageUrl()).isEqualTo("https://img");
        assertThat(saved.getCompatibleGames()).containsExactly("Mario Sports Superstars");
        assertThat(saved.getStatus()).isEqualTo(AmiiboStatus.OWNED);
        assertThat(saved.getNotes()).isEqualTo("nuevo");
        assertThat(saved.getDateAdded()).isEqualTo(LocalDate.of(2026, 10, 13));
        assertThat(saved.getAcquisitionDate()).isEqualTo(LocalDate.of(2024, 5, 1));
        assertThat(saved.getAcquisitionPrice()).isEqualByComparingTo("14.50");
    }

    @Test
    void update_validatesOwnership() {
        Amiibo existing = Amiibo.builder().id("am1").ownerId("u2").build();
        when(amiiboRepository.findById("am1")).thenReturn(Optional.of(existing));
        doThrow(new ForbiddenException("no")).when(ownershipValidator).validateOwner("u2", "u1");

        assertThatThrownBy(() -> service.update("am1", amiibo(), "u1"))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    void update_throwsConflict_whenExternalIdUsedByOther() {
        Amiibo existing = Amiibo.builder().id("am1").ownerId("u1").externalId("old").build();
        Amiibo other = Amiibo.builder().id("am2").externalId("09d0030102bb0e02").build();
        when(amiiboRepository.findById("am1")).thenReturn(Optional.of(existing));
        when(amiiboRepository.findByExternalId("09d0030102bb0e02")).thenReturn(Optional.of(other));

        assertThatThrownBy(() -> service.update("am1", amiibo(), "u1"))
                .isInstanceOf(AmiiboConflictException.class);
        verify(amiiboRepository, never()).save(any(Amiibo.class));
    }

    @Test
    void update_allowsSameExternalId() {
        Amiibo existing = Amiibo.builder().id("am1").ownerId("u1").externalId("09d0030102bb0e02").build();
        when(amiiboRepository.findById("am1")).thenReturn(Optional.of(existing));
        when(amiiboRepository.findByExternalId("09d0030102bb0e02")).thenReturn(Optional.of(existing));
        when(amiiboRepository.save(any(Amiibo.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Amiibo saved = service.update("am1", amiibo(), "u1");

        assertThat(saved.getExternalId()).isEqualTo("09d0030102bb0e02");
        verify(amiiboRepository).save(existing);
    }

    @Test
    void delete_validatesOwnershipAndDeletes() {
        Amiibo existing = Amiibo.builder().id("am1").ownerId("u1").build();
        when(amiiboRepository.findById("am1")).thenReturn(Optional.of(existing));

        service.delete("am1", "u1");

        verify(ownershipValidator).validateOwner("u1", "u1");
        verify(amiiboRepository).deleteById("am1");
    }

    @Test
    void findById_throwsNotFound() {
        when(amiiboRepository.findById("missing")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.findById("missing"))
                .isInstanceOf(AmiiboNotFoundException.class);
    }

    @Test
    void search_resolvesOwnerScope() {
        OwnerScopeResolver.Scope scope = new OwnerScopeResolver.Scope(null, List.of("u2"));
        when(ownerScopeResolver.resolve(CollectionType.AMIIBOS, "all", "u1")).thenReturn(scope);
        when(amiiboRepository.search(any(AmiiboSearchCriteria.class), any()))
                .thenReturn(new PageImpl<>(List.of()));

        service.search(new AmiiboSearchCriteria("mario", AmiiboStatus.OWNED, null, null),
                PageRequest.of(0, 10), "all", "u1");

        ArgumentCaptor<AmiiboSearchCriteria> captor = ArgumentCaptor.forClass(AmiiboSearchCriteria.class);
        verify(amiiboRepository).search(captor.capture(), any());
        assertThat(captor.getValue().name()).isEqualTo("mario");
        assertThat(captor.getValue().status()).isEqualTo(AmiiboStatus.OWNED);
        assertThat(captor.getValue().ownerId()).isNull();
        assertThat(captor.getValue().excludeOwnerIds()).containsExactly("u2");
    }
}
