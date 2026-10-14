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

import com.wikicollection.application.exception.ForbiddenException;
import com.wikicollection.application.exception.FunkoConflictException;
import com.wikicollection.application.exception.FunkoNotFoundException;
import com.wikicollection.domain.model.CollectionType;
import com.wikicollection.domain.model.Funko;
import com.wikicollection.domain.model.FunkoSearchCriteria;
import com.wikicollection.domain.model.FunkoStatus;
import com.wikicollection.domain.model.UserOwned;
import com.wikicollection.domain.port.out.FunkoRepository;

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
class FunkoServiceTest {

    @Mock
    private FunkoRepository funkoRepository;

    @Mock
    private OwnershipValidator ownershipValidator;

    @Mock
    private OwnerScopeResolver ownerScopeResolver;

    @Mock
    private OwnerResolver ownerResolver;

    @InjectMocks
    private FunkoService service;

    private Funko funko() {
        return Funko.builder()
                .externalId("44")
                .name("Batman")
                .status(FunkoStatus.OWNED)
                .build();
    }

    @Test
    void save_setsOwnerAndUserName_andPersists() {
        Funko input = funko();
        UserOwned owned = UserOwned.builder().ownerId("u1").ownerName("Javi").username("javi").build();
        when(funkoRepository.findByExternalId("44")).thenReturn(Optional.empty());
        when(ownerResolver.resolveOwner("u1")).thenReturn(owned);
        when(funkoRepository.save(any(Funko.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Funko saved = service.save(input, "u1");

        assertThat(saved.getOwnerId()).isEqualTo("u1");
        assertThat(saved.getUserOwned()).isEqualTo(owned);
        verify(funkoRepository).save(input);
    }

    @Test
    void save_throwsConflict_whenExternalIdAlreadyExists() {
        Funko input = funko();
        Funko existing = Funko.builder().id("fk-other").externalId("44").build();
        when(funkoRepository.findByExternalId("44")).thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> service.save(input, "u1"))
                .isInstanceOf(FunkoConflictException.class);
        verify(funkoRepository, never()).save(any(Funko.class));
    }

    @Test
    void save_throwsConflict_onDuplicateKey() {
        Funko input = funko();
        when(funkoRepository.findByExternalId("44")).thenReturn(Optional.empty());
        when(funkoRepository.save(any(Funko.class))).thenThrow(new DuplicateKeyException("dup"));

        assertThatThrownBy(() -> service.save(input, "u1"))
                .isInstanceOf(FunkoConflictException.class);
    }

    @Test
    void update_copiesAllUpdatableFieldsAndPersists() {
        Funko existing = Funko.builder()
                .id("fk1")
                .ownerId("u1")
                .externalId("old")
                .name("Old")
                .status(FunkoStatus.WISHLIST)
                .build();
        Funko updates = Funko.builder()
                .externalId("44")
                .name("Batman")
                .series("DC Comics")
                .number("01")
                .category("Comics")
                .exclusive("Walmart")
                .vaulted(true)
                .releaseYear(2012)
                .franchise("DC")
                .popType("Diamond")
                .rarityScore(30)
                .imageUrl("https://img")
                .status(FunkoStatus.OWNED)
                .notes("nuevo")
                .dateAdded(LocalDate.of(2026, 10, 13))
                .acquisitionDate(LocalDate.of(2024, 5, 1))
                .acquisitionPrice(new BigDecimal("24.99"))
                .build();
        when(funkoRepository.findById("fk1")).thenReturn(Optional.of(existing));
        when(funkoRepository.findByExternalId("44")).thenReturn(Optional.empty());
        when(funkoRepository.save(any(Funko.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Funko saved = service.update("fk1", updates, "u1");

        verify(ownershipValidator).validateOwner("u1", "u1");
        assertThat(saved.getId()).isEqualTo("fk1");
        assertThat(saved.getOwnerId()).isEqualTo("u1");
        assertThat(saved.getExternalId()).isEqualTo("44");
        assertThat(saved.getName()).isEqualTo("Batman");
        assertThat(saved.getSeries()).isEqualTo("DC Comics");
        assertThat(saved.getNumber()).isEqualTo("01");
        assertThat(saved.getCategory()).isEqualTo("Comics");
        assertThat(saved.getExclusive()).isEqualTo("Walmart");
        assertThat(saved.getVaulted()).isTrue();
        assertThat(saved.getReleaseYear()).isEqualTo(2012);
        assertThat(saved.getFranchise()).isEqualTo("DC");
        assertThat(saved.getPopType()).isEqualTo("Diamond");
        assertThat(saved.getRarityScore()).isEqualTo(30);
        assertThat(saved.getImageUrl()).isEqualTo("https://img");
        assertThat(saved.getStatus()).isEqualTo(FunkoStatus.OWNED);
        assertThat(saved.getNotes()).isEqualTo("nuevo");
        assertThat(saved.getDateAdded()).isEqualTo(LocalDate.of(2026, 10, 13));
        assertThat(saved.getAcquisitionDate()).isEqualTo(LocalDate.of(2024, 5, 1));
        assertThat(saved.getAcquisitionPrice()).isEqualByComparingTo("24.99");
    }

    @Test
    void update_validatesOwnership() {
        Funko existing = Funko.builder().id("fk1").ownerId("u2").build();
        when(funkoRepository.findById("fk1")).thenReturn(Optional.of(existing));
        doThrow(new ForbiddenException("no")).when(ownershipValidator).validateOwner("u2", "u1");

        assertThatThrownBy(() -> service.update("fk1", funko(), "u1"))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    void update_throwsConflict_whenExternalIdUsedByOther() {
        Funko existing = Funko.builder().id("fk1").ownerId("u1").externalId("old").build();
        Funko other = Funko.builder().id("fk2").externalId("44").build();
        when(funkoRepository.findById("fk1")).thenReturn(Optional.of(existing));
        when(funkoRepository.findByExternalId("44")).thenReturn(Optional.of(other));

        assertThatThrownBy(() -> service.update("fk1", funko(), "u1"))
                .isInstanceOf(FunkoConflictException.class);
        verify(funkoRepository, never()).save(any(Funko.class));
    }

    @Test
    void update_allowsSameExternalId() {
        Funko existing = Funko.builder().id("fk1").ownerId("u1").externalId("44").build();
        when(funkoRepository.findById("fk1")).thenReturn(Optional.of(existing));
        when(funkoRepository.findByExternalId("44")).thenReturn(Optional.of(existing));
        when(funkoRepository.save(any(Funko.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Funko saved = service.update("fk1", funko(), "u1");

        assertThat(saved.getExternalId()).isEqualTo("44");
        verify(funkoRepository).save(existing);
    }

    @Test
    void delete_validatesOwnershipAndDeletes() {
        Funko existing = Funko.builder().id("fk1").ownerId("u1").build();
        when(funkoRepository.findById("fk1")).thenReturn(Optional.of(existing));

        service.delete("fk1", "u1");

        verify(ownershipValidator).validateOwner("u1", "u1");
        verify(funkoRepository).deleteById("fk1");
    }

    @Test
    void findById_throwsNotFound() {
        when(funkoRepository.findById("missing")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.findById("missing"))
                .isInstanceOf(FunkoNotFoundException.class);
    }

    @Test
    void search_resolvesOwnerScope() {
        OwnerScopeResolver.Scope scope = new OwnerScopeResolver.Scope(null, List.of("u2"));
        when(ownerScopeResolver.resolve(CollectionType.FUNKOS, "all", "u1")).thenReturn(scope);
        when(funkoRepository.search(any(FunkoSearchCriteria.class), any()))
                .thenReturn(new PageImpl<>(List.of()));

        service.search(new FunkoSearchCriteria("batman", FunkoStatus.OWNED, null, null),
                PageRequest.of(0, 10), "all", "u1");

        ArgumentCaptor<FunkoSearchCriteria> captor = ArgumentCaptor.forClass(FunkoSearchCriteria.class);
        verify(funkoRepository).search(captor.capture(), any());
        assertThat(captor.getValue().name()).isEqualTo("batman");
        assertThat(captor.getValue().status()).isEqualTo(FunkoStatus.OWNED);
        assertThat(captor.getValue().ownerId()).isNull();
        assertThat(captor.getValue().excludeOwnerIds()).containsExactly("u2");
    }
}
