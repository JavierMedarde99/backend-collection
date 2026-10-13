package com.wikicollection.infrastructure.adapter.out.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.List;

import com.wikicollection.domain.model.Amiibo;
import com.wikicollection.domain.model.AmiiboReleaseDates;
import com.wikicollection.domain.model.AmiiboStatus;
import com.wikicollection.domain.model.AmiiboType;

import org.junit.jupiter.api.Test;

class AmiiboEntityMapperTest {

    private final AmiiboEntityMapper mapper = new AmiiboEntityMapper();

    private Amiibo fullAmiibo() {
        return Amiibo.builder()
                .id("am1")
                .ownerId("u1")
                .externalId("09d0030102bb0e02")
                .name("Metal Mario")
                .character("Metal Mario")
                .amiiboSeries("Mario Sports Superstars")
                .gameSeries("Mario Sports Superstars")
                .amiiboType(AmiiboType.FIGURE)
                .releaseDates(new AmiiboReleaseDates(
                        LocalDate.of(2017, 6, 16),
                        LocalDate.of(2017, 6, 16),
                        LocalDate.of(2018, 7, 20),
                        LocalDate.of(2017, 6, 16)))
                .imageUrl("https://img")
                .compatibleGames(List.of("Mario Sports Superstars", "Super Smash Bros. Ultimate"))
                .status(AmiiboStatus.OWNED)
                .notes("Mi amiibo")
                .dateAdded(LocalDate.of(2026, 10, 13))
                .build();
    }

    @Test
    void toEntity_flattensReleaseDates() {
        AmiiboEntity entity = mapper.toEntity(fullAmiibo());

        assertThat(entity.getReleaseDateAu()).isEqualTo(LocalDate.of(2017, 6, 16));
        assertThat(entity.getReleaseDateEu()).isEqualTo(LocalDate.of(2017, 6, 16));
        assertThat(entity.getReleaseDateJp()).isEqualTo(LocalDate.of(2018, 7, 20));
        assertThat(entity.getReleaseDateNa()).isEqualTo(LocalDate.of(2017, 6, 16));
    }

    @Test
    void toDomain_rebuildsReleaseDates() {
        AmiiboEntity entity = AmiiboEntity.builder()
                .releaseDateAu(LocalDate.of(2017, 6, 16))
                .releaseDateEu(LocalDate.of(2017, 6, 16))
                .releaseDateJp(LocalDate.of(2018, 7, 20))
                .releaseDateNa(LocalDate.of(2017, 6, 16))
                .build();

        assertThat(mapper.toDomain(entity).getReleaseDates())
                .isEqualTo(new AmiiboReleaseDates(
                        LocalDate.of(2017, 6, 16),
                        LocalDate.of(2017, 6, 16),
                        LocalDate.of(2018, 7, 20),
                        LocalDate.of(2017, 6, 16)));
    }

    @Test
    void toDomain_nullReleaseDates_whenAllRegionFieldsNull() {
        AmiiboEntity entity = AmiiboEntity.builder().name("Mario").build();

        assertThat(mapper.toDomain(entity).getReleaseDates()).isNull();
    }

    @Test
    void toEntity_nullReleaseDates_leavesRegionFieldsNull() {
        Amiibo amiibo = Amiibo.builder().name("Mario").build();

        AmiiboEntity entity = mapper.toEntity(amiibo);

        assertThat(entity.getReleaseDateAu()).isNull();
        assertThat(entity.getReleaseDateEu()).isNull();
        assertThat(entity.getReleaseDateJp()).isNull();
        assertThat(entity.getReleaseDateNa()).isNull();
    }

    @Test
    void roundTripsAllFields() {
        Amiibo amiibo = fullAmiibo();

        Amiibo domain = mapper.toDomain(mapper.toEntity(amiibo));

        assertThat(domain).usingRecursiveComparison().isEqualTo(amiibo);
    }

    @Test
    void nullInputs_returnNull() {
        assertThat(mapper.toEntity(null)).isNull();
        assertThat(mapper.toDomain(null)).isNull();
    }
}
