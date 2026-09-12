package com.wikicollection.infrastructure.adapter.out.tmdb;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import com.wikicollection.domain.model.MovieMediaType;
import com.wikicollection.domain.model.MovieSearchResult;

import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;

class TmdbClientTest {

    private MockWebServer server;
    private TmdbClient client;

    @BeforeEach
    void setUp() throws Exception {
        server = new MockWebServer();
        server.start();
        RestClient restClient = RestClient.builder().baseUrl(server.url("").toString()).build();
        client = new TmdbClient(restClient, "", 0, 0);
    }

    @AfterEach
    void tearDown() throws Exception {
        server.shutdown();
    }

    @Test
    void search_mergesMoviesAndTv() throws Exception {
        server.enqueue(new MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                .setBody(movieFixture()));
        server.enqueue(new MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                .setBody(tvFixture()));

        List<MovieSearchResult> results = client.search("club");

        assertThat(results).hasSize(2);
        MovieSearchResult movie = results.get(0);
        assertThat(movie.externalId()).isEqualTo("550");
        assertThat(movie.title()).isEqualTo("Fight Club");
        assertThat(movie.posterUrl()).isEqualTo("https://image.tmdb.org/t/p/w500/poster.jpg");
        assertThat(movie.backdropUrl()).isEqualTo("https://image.tmdb.org/t/p/original/backdrop.jpg");
        assertThat(movie.voteAverage()).isEqualTo(8.4);
        assertThat(movie.mediaType()).isEqualTo(MovieMediaType.MOVIE);
        assertThat(movie.releaseDate()).hasToString("1999-10-15");
        MovieSearchResult tv = results.get(1);
        assertThat(tv.title()).isEqualTo("Breaking Bad");
        assertThat(tv.mediaType()).isEqualTo(MovieMediaType.TV);
        assertThat(tv.releaseDate()).hasToString("2008-01-20");

        RecordedRequest movieRequest = server.takeRequest();
        assertThat(movieRequest.getPath()).startsWith("/search/movie");
        assertThat(movieRequest.getPath()).contains("query=club");
        RecordedRequest tvRequest = server.takeRequest();
        assertThat(tvRequest.getPath()).startsWith("/search/tv");
    }

    @Test
    void search_returnsEmpty_whenBlankQuery() {
        assertThat(client.search("  ")).isEmpty();
        assertThat(client.search(null)).isEmpty();
        assertThat(server.getRequestCount()).isEqualTo(0);
    }

    @Test
    void search_withMovieType_onlyCallsMovieEndpoint() throws Exception {
        server.enqueue(new MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                .setBody(movieFixture()));

        List<MovieSearchResult> results = client.search("club", MovieMediaType.MOVIE);

        assertThat(results).hasSize(1);
        assertThat(results.get(0).mediaType()).isEqualTo(MovieMediaType.MOVIE);
        assertThat(server.getRequestCount()).isEqualTo(1);
        assertThat(server.takeRequest().getPath()).startsWith("/search/movie");
    }

    @Test
    void search_withTvType_onlyCallsTvEndpoint() throws Exception {
        server.enqueue(new MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                .setBody(tvFixture()));

        List<MovieSearchResult> results = client.search("breaking", MovieMediaType.TV);

        assertThat(results).hasSize(1);
        assertThat(results.get(0).mediaType()).isEqualTo(MovieMediaType.TV);
        assertThat(server.getRequestCount()).isEqualTo(1);
        assertThat(server.takeRequest().getPath()).startsWith("/search/tv");
    }

    @Test
    void search_returnsEmpty_onUnauthorized() throws Exception {
        server.enqueue(new MockResponse().setResponseCode(401));

        assertThat(client.search("club")).isEmpty();
    }

    @Test
    void search_returnsEmpty_whenServiceUnreachable() {
        RestClient unreachable = RestClient.builder().baseUrl("http://localhost:1").build();
        TmdbClient offline = new TmdbClient(unreachable, "", 0, 0);

        assertThat(offline.search("club")).isEmpty();
    }

    @Test
    void search_sendsApiKey_whenConfigured() throws Exception {
        server.enqueue(new MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                .setBody(movieFixture()));
        server.enqueue(new MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                .setBody(tvFixture()));
        RestClient restClient = RestClient.builder().baseUrl(server.url("").toString()).build();
        TmdbClient keyed = new TmdbClient(restClient, "secret", 0, 0);

        assertThat(keyed.search("club")).hasSize(2);

        RecordedRequest request = server.takeRequest();
        assertThat(request.getPath()).contains("api_key=secret");
    }

    @Test
    void search_skipsNullBody_andMapsMissingFields() throws Exception {
        server.enqueue(new MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                .setBody(""));
        server.enqueue(new MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                .setBody(partialFixture()));

        List<MovieSearchResult> results = client.search("club");

        assertThat(results).hasSize(2);
        assertThat(results.get(0).externalId()).isNull();
        assertThat(results.get(0).posterUrl()).isNull();
        assertThat(results.get(0).releaseDate()).isNull();
        assertThat(results.get(1).releaseDate()).isNull();
    }

    private String partialFixture() {
        return """
                {
                  "page": 1,
                  "results": [
                    {
                      "overview": "Sin id ni fecha válida",
                      "release_date": "",
                      "vote_average": 5.0
                    },
                    {
                      "id": 7,
                      "title": "Rara",
                      "release_date": "not-a-date",
                      "vote_average": 1.0
                    }
                  ],
                  "total_results": 2
                }
                """;
    }
    @Test
    void search_returnsEmpty_whenReadTimeoutExceeded() throws Exception {
        server.enqueue(new MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                .setBody(movieFixture())
                .setBodyDelay(2, java.util.concurrent.TimeUnit.SECONDS));
        com.wikicollection.infrastructure.config.HttpClientProperties properties =
                new com.wikicollection.infrastructure.config.HttpClientProperties();
        properties.setReadTimeout(java.time.Duration.ofMillis(100));
        RestClient timedClient = properties.restClientBuilder(server.url("").toString()).build();
        TmdbClient impatient = new TmdbClient(timedClient, "", 0, 0);

        assertThat(impatient.search("club")).isEmpty();
    }

    @Test
    void retriesAndReturnsEmpty_whenUnavailable() throws Exception {
        for (int i = 0; i < 4; i++) {
            server.enqueue(new MockResponse().setResponseCode(503));
        }
        RestClient restClient = RestClient.builder().baseUrl(server.url("").toString()).build();
        TmdbClient retrying = new TmdbClient(restClient, "", 3, 0);

        assertThat(retrying.search("club")).isEmpty();
        assertThat(server.getRequestCount()).isEqualTo(4);
    }

    private String movieFixture() {
        return """
                {
                  "page": 1,
                  "results": [
                    {
                      "id": 550,
                      "title": "Fight Club",
                      "overview": "Un oficinista...",
                      "release_date": "1999-10-15",
                      "poster_path": "/poster.jpg",
                      "backdrop_path": "/backdrop.jpg",
                      "vote_average": 8.4
                    }
                  ],
                  "total_results": 1
                }
                """;
    }

    private String tvFixture() {
        return """
                {
                  "page": 1,
                  "results": [
                    {
                      "id": 1396,
                      "name": "Breaking Bad",
                      "overview": "Un profesor...",
                      "first_air_date": "2008-01-20",
                      "poster_path": "/bb.jpg",
                      "backdrop_path": null,
                      "vote_average": 9.5
                    }
                  ],
                  "total_results": 1
                }
                """;
    }
}
