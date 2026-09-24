package com.wikicollection.infrastructure.adapter.out.tmdb;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class ProviderUrlMapperTest {

    private final ProviderUrlMapper mapper = new ProviderUrlMapper();

    @Test
    void buildLogoUrl_returnsFullUrl() {
        assertThat(mapper.buildLogoUrl("/netflix.jpg"))
                .isEqualTo("https://image.tmdb.org/t/p/original/netflix.jpg");
    }

    @Test
    void buildLogoUrl_returnsNull_whenBlank() {
        assertThat(mapper.buildLogoUrl(null)).isNull();
        assertThat(mapper.buildLogoUrl("  ")).isNull();
    }
}
