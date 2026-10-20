package com.wikicollection.infrastructure.adapter.out.rawg;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

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
        assertThat(result.platform()).isEqualTo("PC");
        assertThat(result.publisher()).isEqualTo("CD Projekt Red");
        assertThat(result.developer()).isEqualTo("CD Projekt Red");
        assertThat(result.releaseDate().toString()).isEqualTo("2015-05-18");
        assertThat(result.thumbnailUrl()).isEqualTo("https://media.rawg.io/games/.../witcher3.jpg");
        assertThat(result.externalSource()).isEqualTo("RAWG");

        RecordedRequest request = server.takeRequest();
        assertThat(request.getPath()).startsWith("/games?search=witcher");
        assertThat(request.getPath()).contains("key=my-secret-key");
        assertThat(request.getPath()).doesNotContain("page_size");
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
    void search_keepsWebBrowserPlatformInsteadOfDiscardingIt() throws Exception {
        server.enqueue(new MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                .setBody(rawgFixtureWithPlatform("Web browser")));

        List<GameSearchResult> results = client.search("web");

        assertThat(results.get(0).platform()).isEqualTo("Web browser");
    }

    @Test
    void search_keepsFirstPlatformWhenSeveralPresent() throws Exception {
        server.enqueue(new MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                .setBody(rawgFixtureWithTwoPlatforms()));

        List<GameSearchResult> results = client.search("cross");

        assertThat(results.get(0).platform()).isEqualTo("PC");
    }

    @Test
    void search_keepsConsolePlatformNamesVerbatim() throws Exception {
        server.enqueue(new MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                .setBody(rawgFixtureWithPlatform("PlayStation 2")));
        assertThat(client.search("ps2").get(0).platform()).isEqualTo("PlayStation 2");

        server.enqueue(new MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                .setBody(rawgFixtureWithPlatform("PlayStation 3")));
        assertThat(client.search("ps3").get(0).platform()).isEqualTo("PlayStation 3");

        server.enqueue(new MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                .setBody(rawgFixtureWithPlatform("Wii U")));
        assertThat(client.search("wiiu").get(0).platform()).isEqualTo("Wii U");

        server.enqueue(new MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                .setBody(rawgFixtureWithPlatform("Nintendo Switch")));
        assertThat(client.search("switch").get(0).platform()).isEqualTo("Nintendo Switch");
    }

    @Test
    void search_keepsPlatformOutsideTheOldFiveValuesEnum() throws Exception {
        server.enqueue(new MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                .setBody(rawgFixtureWithPlatform("PlayStation 5")));

        List<GameSearchResult> results = client.search("ps5");

        assertThat(results.get(0).platform()).isEqualTo("PlayStation 5");
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

    @Test
    void search_sendsExcludeAdditionsSoTheCatalogStopsOfferingDlcs() throws Exception {
        server.enqueue(new MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                .setBody("{\"results\":[]}"));

        client.search("witcher");

        RecordedRequest request = server.takeRequest();
        assertThat(request.getPath()).contains("exclude_additions=true");
    }

    @Test
    void getAdditions_mapsAdditionsAndExposesRawgTotalAvailable() throws Exception {
        server.enqueue(new MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                .setBody(additionsFixture()));

        var page = client.getAdditions("292030");

        assertThat(page.totalAvailable()).isEqualTo(12);
        assertThat(page.additions()).hasSize(1);
        GameSearchResult addition = page.additions().get(0);
        assertThat(addition.id()).isEqualTo("355880");
        assertThat(addition.title()).isEqualTo("The Witcher 3: Blood and Wine");
        assertThat(addition.genre()).isEqualTo("RPG");
        assertThat(addition.platform()).isEqualTo("PC");
        assertThat(addition.releaseDate().toString()).isEqualTo("2016-05-31");
        assertThat(addition.thumbnailUrl()).isEqualTo("https://media.rawg.io/games/.../blood-and-wine.jpg");
        assertThat(addition.externalSource()).isEqualTo("RAWG");
    }

    @Test
    void getAdditions_requestsOnePageOfFortyAndSendsTheKey() throws Exception {
        server.enqueue(new MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                .setBody("{\"count\":0,\"results\":[]}"));

        client.getAdditions("292030");

        RecordedRequest request = server.takeRequest();
        assertThat(request.getPath()).startsWith("/games/292030/additions");
        assertThat(request.getPath()).contains("page_size=40");
        assertThat(request.getPath()).contains("key=my-secret-key");
    }

    @Test
    void getAdditions_returnsEmpty_whenRawgHasNoAdditions() throws Exception {
        server.enqueue(new MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                .setBody("{\"count\":0,\"results\":[]}"));

        var page = client.getAdditions("12345");

        assertThat(page.additions()).isEmpty();
        assertThat(page.totalAvailable()).isZero();
    }

    @Test
    void getAdditions_throwsExternalCatalogException_onServerError() {
        server.enqueue(new MockResponse().setResponseCode(500));

        org.assertj.core.api.Assertions.assertThatThrownBy(() -> client.getAdditions("292030"))
                .isInstanceOf(com.wikicollection.application.exception.ExternalCatalogException.class);
    }

    @Test
    void getAdditions_returnsEmptyPage_forNullOrBlankExternalId() {
        assertThat(client.getAdditions(null).additions()).isEmpty();
        assertThat(client.getAdditions("  ").additions()).isEmpty();
    }

    private String additionsFixture() {
        return """
                {
                  "count": 12,
                  "next": null,
                  "previous": null,
                  "results": [
                    {
                      "id": 355880,
                      "name": "The Witcher 3: Blood and Wine",
                      "released": "2016-05-31",
                      "background_image": "https://media.rawg.io/games/.../blood-and-wine.jpg",
                      "genres": [{"id": 3, "name": "RPG"}],
                      "platforms": [{"platform": {"id": 4, "name": "PC"}}]
                    }
                  ]
                }
                """;
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