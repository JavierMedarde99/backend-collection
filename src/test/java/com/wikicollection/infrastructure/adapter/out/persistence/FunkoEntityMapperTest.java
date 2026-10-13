package com.wikicollection.infrastructure.adapter.out.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;

import com.wikicollection.domain.model.Funko;
import com.wikicollection.domain.model.FunkoStatus;

import org.junit.jupiter.api.Test;

class FunkoEntityMapperTest {

    private final FunkoEntityMapper mapper = new FunkoEntityMapper();

    private Funko fullFunko() {
        return Funko.builder()
                .id("fk1")
                .ownerId("u1")
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
                .imageUrl("https://images.hobbydb.com/x.jpg")
                .status(FunkoStatus.OWNED)
                .notes("Mi pop")
                .dateAdded(LocalDate.of(2026, 10, 13))
                .build();
    }

    @Test
    void toEntity_mapsAllFields() {
        FunkoEntity entity = mapper.toEntity(fullFunko());

        assertThat(entity.getId()).isEqualTo("fk1");
        assertThat(entity.getOwnerId()).isEqualTo("u1");
        assertThat(entity.getExternalId()).isEqualTo("44");
        assertThat(entity.getName()).isEqualTo("Batman");
        assertThat(entity.getSeries()).isEqualTo("DC Comics");
        assertThat(entity.getNumber()).isEqualTo("01");
        assertThat(entity.getCategory()).isEqualTo("Comics");
        assertThat(entity.getExclusive()).isEqualTo("Walmart");
        assertThat(entity.getVaulted()).isTrue();
        assertThat(entity.getReleaseYear()).isEqualTo(2012);
        assertThat(entity.getFranchise()).isEqualTo("DC");
        assertThat(entity.getPopType()).isEqualTo("Diamond");
        assertThat(entity.getRarityScore()).isEqualTo(30);
        assertThat(entity.getStatus()).isEqualTo(FunkoStatus.OWNED);
        assertThat(entity.getNotes()).isEqualTo("Mi pop");
        assertThat(entity.getDateAdded()).isEqualTo(LocalDate.of(2026, 10, 13));
    }

    @Test
    void roundTripsAllFields() {
        Funko funko = fullFunko();

        Funko domain = mapper.toDomain(mapper.toEntity(funko));

        assertThat(domain).usingRecursiveComparison().isEqualTo(funko);
    }

    @Test
    void nullInputs_returnNull() {
        assertThat(mapper.toEntity(null)).isNull();
        assertThat(mapper.toDomain(null)).isNull();
    }
}
