package com.wikicollection.infrastructure.adapter.out.rawg;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import com.wikicollection.domain.model.PlatformInfo;

import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;

class RawgPlatformsClientTest {

    private MockWebServer server;
    private RawgPlatformsClient client;

    @BeforeEach
    void setUp() throws Exception {
        server = new MockWebServer();
        server.start();
        RestClient restClient = RestClient.builder()
                .baseUrl(server.url("/").toString())
                .build();
        client = new RawgPlatformsClient(restClient, "my-secret-key");
    }

    @AfterEach
    void tearDown() throws Exception {
        server.shutdown();
    }

    @Test
    void getPlatforms_mapsIdNameAndSlug() throws Exception {
        server.enqueue(json(platformsFixture()));

        List<PlatformInfo> platforms = client.getPlatforms();

        assertThat(platforms).hasSize(3);
        assertThat(platforms.get(0).id()).isEqualTo(1L);
        assertThat(platforms.get(0).name()).isEqualTo("PlayStation 5");
        assertThat(platforms.get(0).slug()).isEqualTo("ps5");
        assertThat(platforms.get(2).name()).isEqualTo("Nintendo Switch");
        assertThat(platforms.get(2).slug()).isEqualTo("nintendo-switch");
    }

    @Test
    void getPlatforms_hitsPlatformsPath_withApiKey() throws Exception {
        server.enqueue(json(platformsFixture()));

        client.getPlatforms();

        RecordedRequest request = server.takeRequest();
        assertThat(request.getPath()).isEqualTo("/platforms?key=my-secret-key");
    }

    @Test
    void getPlatforms_keepsPlatformNamesOutsideTheOldEnum() throws Exception {
        server.enqueue(json("""
                {"count":2,"results":[
                  {"id":1,"name":"Web browser","slug":"web"},
                  {"id":2,"name":"PlayStation 5","slug":"ps5"}
                ]}
                """));

        List<PlatformInfo> platforms = client.getPlatforms();

        assertThat(platforms).extracting(PlatformInfo::name)
                .containsExactly("Web browser", "PlayStation 5");
    }

    @Test
    void getPlatforms_returnsEmpty_whenServerError() throws Exception {
        server.enqueue(new MockResponse().setResponseCode(500));

        assertThat(client.getPlatforms()).isEmpty();
    }

    @Test
    void getPlatforms_returnsEmpty_whenNoResults() throws Exception {
        server.enqueue(json("{\"count\":0,\"results\":[]}"));

        assertThat(client.getPlatforms()).isEmpty();
    }

    private MockResponse json(String body) {
        return new MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                .setBody(body);
    }

    private String platformsFixture() {
        return """
                {
                  "count": 3,
                  "results": [
                    { "id": 1, "name": "PlayStation 5", "slug": "ps5" },
                    { "id": 2, "name": "Xbox Series X", "slug": "xbox-series-x" },
                    { "id": 3, "name": "Nintendo Switch", "slug": "nintendo-switch" }
                  ]
                }
                """;
    }
}