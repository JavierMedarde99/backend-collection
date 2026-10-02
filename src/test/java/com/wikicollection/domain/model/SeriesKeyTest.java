package com.wikicollection.domain.model;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class SeriesKeyTest {

    @Test
    void normalizaAMinusculasYSinEspaciosSobrantes() {
        assertThat(SeriesKey.normalize("Harry Potter")).isEqualTo("harry potter");
        assertThat(SeriesKey.normalize("  The   Hobbit  ")).isEqualTo("the hobbit");
    }

    @Test
    void esInsensibleAMayusculas() {
        assertThat(SeriesKey.normalize("Harry Potter"))
                .isEqualTo(SeriesKey.normalize("HARRY POTTER"))
                .isEqualTo(SeriesKey.normalize("harry potter"));
    }

    @Test
    void colapsaEspaciosInternos() {
        assertThat(SeriesKey.normalize("One   Piece")).isEqualTo(SeriesKey.normalize("one piece"));
    }

    @Test
    void devuelveNullSiNoHayNombre() {
        assertThat(SeriesKey.normalize(null)).isNull();
        assertThat(SeriesKey.normalize("")).isNull();
        assertThat(SeriesKey.normalize("   ")).isNull();
    }

    @Test
    void distingueSeriesDistintas() {
        assertThat(SeriesKey.normalize("Dune")).isNotEqualTo(SeriesKey.normalize("Dune Messiah"));
    }
}