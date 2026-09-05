package com.wikicollection.infrastructure.adapter.out.freetogame;

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

class FreeToGameClientTest {

    private MockWebServer server;
    private FreeToGameClient client;

    @BeforeEach
    void setUp() throws Exception {
        server = new MockWebServer();
        server.start();
        RestClient restClient = RestClient.builder()
                .baseUrl(server.url("/").toString())
                .build();
        client = new FreeToGameClient(restClient);
    }

    @AfterEach
    void tearDown() throws Exception {
        server.shutdown();
    }

    @Test
    void search_mapsFreeToGameResponse() throws Exception {
        server.enqueue(new MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                .setBody(freeToGameFixture()));

        List<GameSearchResult> results = client.search("warzone");

        assertThat(results).hasSize(1);
        GameSearchResult result = results.get(0);
        assertThat(result.id()).isEqualTo("452");
        assertThat(result.title()).isEqualTo("Call of Duty: Warzone");
        assertThat(result.description()).isEqualTo("El battle royale gratuito");
        assertThat(result.genre()).isEqualTo("Shooter");
        assertThat(result.platform()).isEqualTo(GamePlatform.PC);
        assertThat(result.publisher()).isEqualTo("Activision");
        assertThat(result.developer()).isEqualTo("Infinity Ward");
        assertThat(result.releaseDate().toString()).isEqualTo("2020-03-10");
        assertThat(result.thumbnailUrl()).isEqualTo("https://www.freetogame.com/g/452/thumbnail.jpg");
        assertThat(result.externalSource()).isEqualTo("FreeToGame");

        RecordedRequest request = server.takeRequest();
        assertThat(request.getPath()).startsWith("/games?title=warzone");
    }

    @Test
    void search_mapsWebBrowserPlatformToNull() throws Exception {
        server.enqueue(new MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                .setBody(freeToGameFixtureWithPlatform("Web Browser")));

        List<GameSearchResult> results = client.search("browser");

        assertThat(results.get(0).platform()).isNull();
    }

    @Test
    void search_mapsPcWhenPcAndWebPresent() throws Exception {
        server.enqueue(new MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                .setBody(freeToGameFixtureWithPlatform("PC (Windows), Web Browser")));

        List<GameSearchResult> results = client.search("cross");

        assertThat(results.get(0).platform()).isEqualTo(GamePlatform.PC);
    }

    @Test
    void search_returnsEmpty_whenEmptyBody() throws Exception {
        server.enqueue(new MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                .setBody("[]"));

        List<GameSearchResult> results = client.search("vacio");

        assertThat(results).isEmpty();
    }

    @Test
    void search_returnsEmpty_whenServerError() throws Exception {
        server.enqueue(new MockResponse().setResponseCode(500));

        List<GameSearchResult> results = client.search("warzone");

        assertThat(results).isEmpty();
    }

    @Test
    void getAllGames_mapsResponse() throws Exception {
        server.enqueue(new MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                .setBody(freeToGameFixture()));

        List<GameSearchResult> results = client.getAllGames();

        assertThat(results).hasSize(1);
        RecordedRequest request = server.takeRequest();
        assertThat(request.getPath()).isEqualTo("/games");
    }

    @Test
    void getAllGames_returnsEmpty_whenServerError() throws Exception {
        server.enqueue(new MockResponse().setResponseCode(503));

        List<GameSearchResult> results = client.getAllGames();

        assertThat(results).isEmpty();
    }

    @Test
    void search_returnsEmpty_whenConnectionFails() throws Exception {
        server.shutdown();

        List<GameSearchResult> results = client.search("warzone");

        assertThat(results).isEmpty();
    }

    @Test
    void getAllGames_returnsEmpty_whenConnectionFails() throws Exception {
        server.shutdown();

        List<GameSearchResult> results = client.getAllGames();

        assertThat(results).isEmpty();
    }

    @Test
    void search_returnsEmpty_whenNoContent() throws Exception {
        server.enqueue(new MockResponse().setResponseCode(204));

        List<GameSearchResult> results = client.search("warzone");

        assertThat(results).isEmpty();
    }

    @Test
    void search_handlesMissingAndUnknownFields() throws Exception {
        server.enqueue(new MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                .setBody("""
                        [
                          {
                            "title": "Juego Anónimo",
                            "thumbnail": "http://thumb",
                            "short_description": "Descripción",
                            "genre": "Action",
                            "platform": "Console",
                            "release_date": ""
                          }
                        ]
                        """));

        List<GameSearchResult> results = client.search("anonimo");

        assertThat(results).hasSize(1);
        GameSearchResult result = results.get(0);
        assertThat(result.id()).isNull();
        assertThat(result.platform()).isNull();
        assertThat(result.releaseDate()).isNull();
    }

    @Test
    void search_handlesMissingPlatformField() throws Exception {
        server.enqueue(new MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                .setBody("""
                        [
                          {
                            "title": "Juego Sin Plataforma",
                            "short_description": "Descripción",
                            "genre": "Action"
                          }
                        ]
                        """));

        List<GameSearchResult> results = client.search("plataforma");

        assertThat(results).hasSize(1);
        assertThat(results.get(0).platform()).isNull();
    }

    @Test
    void search_handlesMalformedReleaseDate() throws Exception {
        server.enqueue(new MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                .setBody("""
                        [
                          {
                            "title": "Juego",
                            "platform": "PC (Windows)",
                            "release_date": "no-es-una-fecha"
                          }
                        ]
                        """));

        List<GameSearchResult> results = client.search("juego");

        assertThat(results.get(0).releaseDate()).isNull();
    }

    private String freeToGameFixture() {
        return """
                [
                  {
                    "id": 452,
                    "title": "Call of Duty: Warzone",
                    "thumbnail": "https://www.freetogame.com/g/452/thumbnail.jpg",
                    "short_description": "El battle royale gratuito",
                    "game_url": "https://www.freetogame.com/open/call-of-duty-warzone",
                    "genre": "Shooter",
                    "platform": "PC (Windows)",
                    "publisher": "Activision",
                    "developer": "Infinity Ward",
                    "release_date": "2020-03-10",
                    "freetogame_profile_url": "https://www.freetogame.com/call-of-duty-warzone"
                  }
                ]
                """;
    }

    private String freeToGameFixtureWithPlatform(String platform) {
        return """
                [
                  {
                    "id": 452,
                    "title": "Juego",
                    "thumbnail": "http://thumb",
                    "short_description": "Descripción",
                    "genre": "Action",
                    "platform": "%s",
                    "publisher": "Pub",
                    "developer": "Dev",
                    "release_date": "2020-03-10"
                  }
                ]
                """.formatted(platform);
    }
}