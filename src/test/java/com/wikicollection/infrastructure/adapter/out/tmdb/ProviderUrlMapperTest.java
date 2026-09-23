package com.wikicollection.infrastructure.adapter.out.tmdb;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class ProviderUrlMapperTest {

    private final ProviderUrlMapper mapper = new ProviderUrlMapper();

    @Test
    void buildDeepLink_returnsSearchUrl_whenMapped() {
        assertThat(mapper.buildDeepLink(10, "Dune"))
                .isEqualTo("https://www.netflix.com/search?q=Dune");
    }

    @Test
    void buildDeepLink_returnsNull_whenUnmapped() {
        assertThat(mapper.buildDeepLink(999999, "Dune")).isNull();
    }
}
