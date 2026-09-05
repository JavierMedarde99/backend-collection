package com.wikicollection.infrastructure.config;

import static org.assertj.core.api.Assertions.assertThat;

import com.wikicollection.domain.model.GameStatus;

import org.junit.jupiter.api.Test;

class StringToGameStatusConverterTest {

    private final StringToGameStatusConverter converter = new StringToGameStatusConverter();

    @Test
    void convert_mapsUpperTrimmedStatus() {
        assertThat(converter.convert(" PLAYING ")).isEqualTo(GameStatus.PLAYING);
    }

    @Test
    void convert_handlesLowercase() {
        assertThat(converter.convert("completed")).isEqualTo(GameStatus.COMPLETED);
    }

    @Test
    void convert_mapsAllValues() {
        for (GameStatus status : GameStatus.values()) {
            assertThat(converter.convert(status.name())).isEqualTo(status);
        }
    }
}