package com.wikicollection.infrastructure.adapter.out.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import com.wikicollection.domain.model.MovieMediaType;
import com.wikicollection.domain.model.MovieShow;
import com.wikicollection.domain.model.MovieStatus;
import com.wikicollection.domain.model.ProviderAccessType;
import com.wikicollection.domain.model.StreamingProvider;

import org.junit.jupiter.api.Test;

class MovieShowEntityMapperTest {

    private final MovieShowEntityMapper mapper = new MovieShowEntityMapper();

    private MovieShow sampleShow() {
        return MovieShow.builder()
                .id("m1")
                .externalId("550")
                .title("Fight Club")
                .mediaType(MovieMediaType.MOVIE)
                .status(MovieStatus.WATCHED)
                .watchCountry("ES")
                .streamingProviders(List.of(
                        StreamingProvider.builder()
                                .providerId(10).providerName("Netflix")
                                .logoUrl("https://image.tmdb.org/t/p/original/netflix.jpg")
                                .type(ProviderAccessType.FLATRATE)
                                .deepLinkUrl("https://www.netflix.com/search?q=Fight%20Club")
                                .build()))
                .build();
    }

    @Test
    void roundTrip_preservesProvidersAndCountry() {
        MovieShow result = mapper.toDomain(mapper.toEntity(sampleShow()));

        assertThat(result.getWatchCountry()).isEqualTo("ES");
        assertThat(result.getStreamingProviders()).hasSize(1);
        assertThat(result.getStreamingProviders().get(0).getProviderName()).isEqualTo("Netflix");
        assertThat(result.getStreamingProviders().get(0).getType()).isEqualTo(ProviderAccessType.FLATRATE);
    }

    @Test
    void mapsNullProviders_whenAbsent() {
        MovieShow show = MovieShow.builder().id("m1").title("Fight Club").build();

        MovieShowEntity entity = mapper.toEntity(show);

        assertThat(entity.getStreamingProviders()).isNull();
        assertThat(entity.getWatchCountry()).isNull();
        assertThat(mapper.toDomain(entity).getStreamingProviders()).isNull();
    }

    @Test
    void mapsNull_whenNull() {
        assertThat(mapper.toEntity(null)).isNull();
        assertThat(mapper.toDomain(null)).isNull();
    }
}
