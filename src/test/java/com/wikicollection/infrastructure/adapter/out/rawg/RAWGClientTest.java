package com.wikicollection.infrastructure.adapter.out.rawg;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import com.wikicollection.domain.model.GamePlatform;
import com.wikicollection.domain.model.GameSearchResult;

import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;

class RAWGClientTest {

    private MockWebServer server;
    private RAWGClient client;

    @BeforeEach
    void setUp() throws Exception {
        server = new MockWebServer();
        server.start();
        RestClient restClient = RestClient.builder()
                .baseUrl(server.url("/").toString())
                .build();
        client = new RAWGClient(restClient, "my-secret-key");
    }

    @AfterEach
    void tearDown() throws Exception {
        server.shutdown();
    }

    @Test
    void search_mapsRawgResponse() throws Exception {
        server.enqueue(new MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                .setBody(rawgFixture()));

        List<GameSearchResult> results = client.search("witcher");

        assertThat(results).hasSize(1);
        GameSearchResult result = results.get(0);
        assertThat(result.id()).isEqualTo("3498");
        assertThat(result.title()).isEqualTo("The Witcher 3: Wild Hunt");
        assertThat(result.genre()).isEqualTo("Action");
        assertThat(result.platform()).isEqualTo(GamePlatform.PC);
        assertThat(result.publisher()).isEqualTo("CD Projekt Red");
        assertThat(result.developer()).isEqualTo("CD Projekt Red");
        assertThat(result.releaseDate().toString()).isEqualTo("2015-05-18");
        assertThat(result.thumbnailUrl()).isEqualTo("https://media.rawg.io/games/.../witcher3.jpg");
        assertThat(result.externalSource()).isEqualTo("RAWG");

        RecordedRequest request = server.takeRequest();
        assertThat(request.getPath()).startsWith("/games?search=witcher");
        assertThat(request.getPath()).contains("key=my-secret-key");
        assertThat(request.getPath()).contains("page_size=10");
    }

    @Test
    void search_withoutApiKey_omitsKeyParam() throws Exception {
        RestClient restClient = RestClient.builder()
                .baseUrl(server.url("/").toString())
                .build();
        RAWGClient clientNoKey = new RAWGClient(restClient, "");
        server.enqueue(new MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                .setBody(rawgFixture()));

        clientNoKey.search("witcher");

        RecordedRequest request = server.takeRequest();
        assertThat(request.getPath()).startsWith("/games?search=witcher");
        assertThat(request.getPath()).doesNotContain("key=");
    }

    @Test
    void search_returnsEmpty_whenNoResults() throws Exception {
        server.enqueue(new MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                .setBody("{\"results\":[]}"));

        List<GameSearchResult> results = client.search("vacio");

        assertThat(results).isEmpty();
    }

    @Test
    void search_returnsEmpty_whenServerError() throws Exception {
        server.enqueue(new MockResponse().setResponseCode(500));

        List<GameSearchResult> results = client.search("witcher");

        assertThat(results).isEmpty();
    }

    @Test
    void getAllGames_mapsRawgResponse() throws Exception {
        server.enqueue(new MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                .setBody(rawgFixture()));

        List<GameSearchResult> results = client.getAllGames();

        assertThat(results).hasSize(1);
        RecordedRequest request = server.takeRequest();
        assertThat(request.getPath()).startsWith("/games?");
        assertThat(request.getPath()).doesNotContain("search=");
        assertThat(request.getPath()).contains("key=my-secret-key");
    }

    @Test
    void getAllGames_returnsEmpty_whenServerError() throws Exception {
        server.enqueue(new MockResponse().setResponseCode(503));

        List<GameSearchResult> results = client.getAllGames();

        assertThat(results).isEmpty();
    }

    @Test
    void search_mapsWebBrowserPlatform() throws Exception {
        server.enqueue(new MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                .setBody(rawgFixtureWithPlatform("Web browser")));

        List<GameSearchResult> results = client.search("web");

        assertThat(results.get(0).platform()).isEqualTo(GamePlatform.WEB_BROWSER);
    }

    @Test
    void search_mapsBothPlatformsToBoth() throws Exception {
        server.enqueue(new MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                .setBody(rawgFixtureWithTwoPlatforms()));

        List<GameSearchResult> results = client.search("cross");

        assertThat(results.get(0).platform()).isEqualTo(GamePlatform.BOTH);
    }

    private String rawgFixture() {
        return """
                {
                  "count": 1,
                  "next": null,
                  "results": [
                    {
                      "id": 3498,
                      "slug": "the-witcher-3-wild-hunt",
                      "name": "The Witcher 3: Wild Hunt",
                      "released": "2015-05-18",
                      "background_image": "https://media.rawg.io/games/.../witcher3.jpg",
                      "genres": [{"id": 4, "name": "Action"}],
                      "platforms": [
                        {"platform": {"id": 4, "name": "PC"}}
                      ],
                      "publishers": [{"id": 540, "name": "CD Projekt Red"}],
                      "developers": [{"id": 175, "name": "CD Projekt Red"}]
                    }
                  ]
                }
                """;
    }

    private String rawgFixtureWithPlatform(String platform) {
        return """
                {
                  "count": 1,
                  "results": [
                    {
                      "id": 1,
                      "name": "Juego Web",
                      "released": null,
                      "platforms": [
                        {"platform": {"id": 5, "name": "%s"}}
                      ]
                    }
                  ]
                }
                """.formatted(platform);
    }

    private String rawgFixtureWithTwoPlatforms() {
        return """
                {
                  "count": 1,
                  "results": [
                    {
                      "id": 2,
                      "name": "Juego Cross",
                      "released": null,
                      "platforms": [
                        {"platform": {"id": 4, "name": "PC"}},
                        {"platform": {"id": 5, "name": "Web"}}
                      ]
                    }
                  ]
                }
                """;
    }
}