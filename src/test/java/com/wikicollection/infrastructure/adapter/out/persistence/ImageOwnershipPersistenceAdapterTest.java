package com.wikicollection.infrastructure.adapter.out.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import com.wikicollection.domain.model.ImageOwnership;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ImageOwnershipPersistenceAdapterTest {

    @Mock
    private SpringDataImageOwnershipRepository springDataRepository;

    @Mock
    private ImageOwnershipEntityMapper mapper;

    @InjectMocks
    private ImageOwnershipPersistenceAdapter adapter;

    @Test
    void save_mapsAndDelegates() {
        ImageOwnership ownership = new ImageOwnership("abc.png", "u1", "Javi");
        ImageOwnershipEntity entity = ImageOwnershipEntity.builder()
                .filename("abc.png").ownerId("u1").ownerName("Javi").build();
        when(mapper.toEntity(ownership)).thenReturn(entity);
        when(springDataRepository.save(entity)).thenReturn(entity);
        when(mapper.toDomain(entity)).thenReturn(ownership);

        assertThat(adapter.save(ownership)).isEqualTo(ownership);
    }

    @Test
    void findByFilename_mapsEntity_whenExists() {
        ImageOwnership ownership = new ImageOwnership("abc.png", "u1", "Javi");
        ImageOwnershipEntity entity = ImageOwnershipEntity.builder()
                .filename("abc.png").ownerId("u1").ownerName("Javi").build();
        when(springDataRepository.findById("abc.png")).thenReturn(Optional.of(entity));
        when(mapper.toDomain(entity)).thenReturn(ownership);

        assertThat(adapter.findByFilename("abc.png")).contains(ownership);
    }

    @Test
    void findByFilename_returnsEmpty_whenMissing() {
        when(springDataRepository.findById("abc.png")).thenReturn(Optional.empty());

        assertThat(adapter.findByFilename("abc.png")).isEmpty();
    }

    @Test
    void deleteByFilename_delegates() {
        adapter.deleteByFilename("abc.png");

        verify(springDataRepository).deleteById("abc.png");
    }
}
