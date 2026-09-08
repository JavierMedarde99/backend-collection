package com.wikicollection.infrastructure.adapter.out.bgg.json;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import com.wikicollection.domain.model.BoardGameSearchResult;
import com.wikicollection.infrastructure.adapter.out.bgg.mapper.BoardGameJsonMapper;

import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestTemplate;

class BggJsonClientTest {

    private MockWebServer server;
    private BggJsonClient client;

    @BeforeEach
    void setUp() throws Exception {
        server = new MockWebServer();
        server.start();
        client = new BggJsonClient(new RestTemplate(), server.url("/api/v1").toString(), new BoardGameJsonMapper());
    }

    @AfterEach
    void tearDown() throws Exception {
        server.shutdown();
    }

    @Test
    void search_mapsBggJsonResponse() throws Exception {
        server.enqueue(new MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                .setBody(bggJsonFixture()));

        List<BoardGameSearchResult> results = client.search("catan");

        assertThat(results).hasSize(1);
        BoardGameSearchResult result = results.get(0);
        assertThat(result.bggId()).isEqualTo("31260");
        assertThat(result.title()).isEqualTo("Catan");
        assertThat(result.description()).isEqualTo("Un juego de colonización");
        assertThat(result.yearPublished()).isEqualTo(2007);
        assertThat(result.minPlayers()).isEqualTo(3);
        assertThat(result.maxPlayers()).isEqualTo(4);
        assertThat(result.minPlaytime()).isEqualTo(60);
        assertThat(result.maxPlaytime()).isEqualTo(120);
        assertThat(result.publisher()).isEqualTo("Kosmos");
        assertThat(result.designers()).containsExactly("Klaus Teuber");
        assertThat(result.categories()).contains("Negociación");
        assertThat(result.mechanics()).contains("Trading");
        assertThat(result.imageUrl()).isEqualTo("http://img");
        assertThat(result.thumbnailUrl()).isEqualTo("http://thumb");
        assertThat(result.bggRating()).isNull();
        assertThat(result.externalSource()).isEqualTo("BGG");

        RecordedRequest request = server.takeRequest();
        assertThat(request.getPath()).startsWith("/api/v1/search?query=catan");
    }

    @Test
    void search_returnsEmpty_whenEmptyBody() throws Exception {
        server.enqueue(new MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                .setBody("[]"));

        List<BoardGameSearchResult> results = client.search("vacio");

        assertThat(results).isEmpty();
    }

    @Test
    void search_returnsEmpty_whenNullBody() throws Exception {
        server.enqueue(new MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                .setBody("null"));

        List<BoardGameSearchResult> results = client.search("nada");

        assertThat(results).isEmpty();
    }

    @Test
    void search_returnsEmpty_whenServerError() throws Exception {
        server.enqueue(new MockResponse().setResponseCode(500));

        List<BoardGameSearchResult> results = client.search("catan");

        assertThat(results).isEmpty();
    }

    @Test
    void search_returnsEmpty_whenConnectionFails() throws Exception {
        server.shutdown();

        List<BoardGameSearchResult> results = client.search("catan");

        assertThat(results).isEmpty();
    }

    @Test
    void search_mapsMissingFieldsToNull() throws Exception {
        server.enqueue(new MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                .setBody("""
                        [
                          {
                            "id": "1",
                            "name": "Juego Sin Datos"
                          }
                        ]
                        """));

        List<BoardGameSearchResult> results = client.search("anonimo");

        assertThat(results).hasSize(1);
        BoardGameSearchResult result = results.get(0);
        assertThat(result.title()).isEqualTo("Juego Sin Datos");
        assertThat(result.publisher()).isNull();
        assertThat(result.designers()).isEmpty();
        assertThat(result.yearPublished()).isNull();
    }

    private String bggJsonFixture() {
        return """
                [
                  {
                    "id": "31260",
                    "name": "Catan",
                    "description": "Un juego de colonización",
                    "year_published": 2007,
                    "min_players": 3,
                    "max_players": 4,
                    "min_playtime": 60,
                    "max_playtime": 120,
                    "publishers": ["Kosmos"],
                    "designers": ["Klaus Teuber"],
                    "categories": ["Negociación"],
                    "mechanics": ["Trading"],
                    "image_url": "http://img",
                    "thumb_url": "http://thumb"
                  }
                ]
                """;
    }
}