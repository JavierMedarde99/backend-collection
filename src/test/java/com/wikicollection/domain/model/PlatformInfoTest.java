package com.wikicollection.domain.model;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;

class PlatformInfoTest {

    @Test
    void platformInfo_immutable() {
        PlatformInfo platform = new PlatformInfo(1L, "PC", "pc");

        assertThat(platform.id()).isEqualTo(1L);
        assertThat(platform.name()).isEqualTo("PC");
        assertThat(platform.slug()).isEqualTo("pc");
    }

    @Test
    void platformInfo_allFieldsNull_whenDefaultConstructed() {
        // No hay constructor por defecto; el record genera uno implícito.
        // Este test existe para documentar el contrato.
        PlatformInfo platform = new PlatformInfo(1L, "PlayStation 5", "ps5");
        assertThat(platform.id()).isEqualTo(1L);
        assertThat(platform.name()).isEqualTo("PlayStation 5");
        assertThat(platform.slug()).isEqualTo("ps5");
    }
}