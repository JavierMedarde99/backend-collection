package com.wikicollection.infrastructure.adapter.out.tmdb;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class ProviderUrlMapperTest {

    private final ProviderUrlMapper mapper = new ProviderUrlMapper();

    @Test
    void buildDeepLink_returnsSearchUrl_whenMapped() {
        assertThat(mapper.buildDeepLink(8, "Dune"))
                .isEqualTo("https://www.netflix.com/search?q=Dune");
    }

    @Test
    void buildDeepLink_returnsNull_whenUnmapped() {
        assertThat(mapper.buildDeepLink(999999, "Dune")).isNull();
    }

    @Test
    void buildDeepLink_encodesTitle() {
        assertThat(mapper.buildDeepLink(337, "El Señor de los Anillos"))
                .isEqualTo("https://www.disneyplus.com/search?q=El%20Se%C3%B1or%20de%20los%20Anillos");
    }

    @Test
    void buildDeepLink_mapsKnownProviders() {
        assertThat(mapper.buildDeepLink(1899, "Dune")).startsWith("https://www.max.com/search?q=");
        assertThat(mapper.buildDeepLink(9, "Dune")).startsWith("https://www.primevideo.com/search?q=");
        assertThat(mapper.buildDeepLink(119, "Dune")).startsWith("https://www.primevideo.com/search?q=");
        assertThat(mapper.buildDeepLink(350, "Dune")).startsWith("https://tv.apple.com/search?q=");
        assertThat(mapper.buildDeepLink(2, "Dune")).startsWith("https://tv.apple.com/search?q=");
        assertThat(mapper.buildDeepLink(11, "Dune")).startsWith("https://mubi.com/en/search?q=");
        assertThat(mapper.buildDeepLink(531, "Dune")).startsWith("https://www.paramountplus.com/search?q=");
    }

    @Test
    void buildDeepLink_returnsNull_whenIdBelongsToAnotherPlatform() {
        assertThat(mapper.buildDeepLink(10, "Dune")).startsWith("https://www.primevideo.com/search?q=");
        assertThat(mapper.buildDeepLink(103, "Dune")).isNull();
        assertThat(mapper.buildDeepLink(153, "Dune")).isNull();
    }

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
