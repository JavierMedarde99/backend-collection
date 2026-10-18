package com.wikicollection.infrastructure.adapter.out.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.wikicollection.domain.model.ImageOwnership;

import org.junit.jupiter.api.Test;

class ImageOwnershipEntityMapperTest {

    private final ImageOwnershipEntityMapper mapper = new ImageOwnershipEntityMapper();

    @Test
    void toEntity_mapsAllFields() {
        ImageOwnershipEntity entity = mapper.toEntity(new ImageOwnership("abc.png", "u1", "Javi"));

        assertThat(entity.getFilename()).isEqualTo("abc.png");
        assertThat(entity.getOwnerId()).isEqualTo("u1");
        assertThat(entity.getOwnerName()).isEqualTo("Javi");
    }

    @Test
    void toDomain_mapsAllFields() {
        ImageOwnership domain = mapper.toDomain(ImageOwnershipEntity.builder()
                .filename("abc.png").ownerId("u1").ownerName("Javi").build());

        assertThat(domain).isEqualTo(new ImageOwnership("abc.png", "u1", "Javi"));
    }

    @Test
    void nullsArePreserved() {
        assertThat(mapper.toEntity(null)).isNull();
        assertThat(mapper.toDomain(null)).isNull();
    }
}
