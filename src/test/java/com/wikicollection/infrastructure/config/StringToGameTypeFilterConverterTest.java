package com.wikicollection.infrastructure.config;

import static org.assertj.core.api.Assertions.assertThat;

import com.wikicollection.domain.model.GameTypeFilter;

import org.junit.jupiter.api.Test;

class StringToGameTypeFilterConverterTest {

    private final StringToGameTypeFilterConverter converter = new StringToGameTypeFilterConverter();

    @Test
    void convert_mapsLowercaseType() {
        assertThat(converter.convert("dlc")).isEqualTo(GameTypeFilter.DLC);
        assertThat(converter.convert("base")).isEqualTo(GameTypeFilter.BASE);
        assertThat(converter.convert("all")).isEqualTo(GameTypeFilter.ALL);
    }

    @Test
    void convert_mapsUppercaseAndTrimmed() {
        assertThat(converter.convert(" BASE ")).isEqualTo(GameTypeFilter.BASE);
    }

    @Test
    void convert_handlesNullAndBlankAsNoFilter() {
        assertThat(converter.convert(null)).isNull();
        assertThat(converter.convert("  ")).isNull();
    }
}
