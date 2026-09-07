package com.wikicollection.infrastructure.adapter.out.steam;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import com.wikicollection.domain.model.SteamAchievement;

import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;

class SteamAchievementsClientTest {

    private MockWebServer storeServer;
    private MockWebServer apiServer;
    private SteamAchievementsClient client;

    @BeforeEach
    void setUp() throws Exception {
        storeServer = new MockWebServer();
        storeServer.start();
        apiServer = new MockWebServer();
        apiServer.start();

        RestClient storeRestClient = RestClient.builder()
                .baseUrl(storeServer.url("/").toString())
                .build();
        RestClient apiRestClient = RestClient.builder()
                .baseUrl(apiServer.url("/").toString())
                .build();

        client = new SteamAchievementsClient(apiRestClient, storeRestClient, "my-secret-key");
    }

    @AfterEach
    void tearDown() throws Exception {
        storeServer.shutdown();
        apiServer.shutdown();
    }

    @Test
    void searchGameByName_returnsFirstAppId() throws Exception {
        storeServer.enqueue(new MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                .setBody(storesearchFixture()));

        Long appId = client.searchGameByName("half-life");

        assertThat(appId).isEqualTo(70L);

        RecordedRequest request = storeServer.takeRequest();
        assertThat(request.getPath()).startsWith("/storesearch?term=half-life");
        assertThat(request.getPath()).contains("l=spanish");
        assertThat(request.getPath()).contains("cc=ES");
    }

    @Test
    void searchGameByName_returnsNull_whenNoResults() throws Exception {
        storeServer.enqueue(new MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                .setBody("{\"total\":0,\"items\":[]}"));

        assertThat(client.searchGameByName("juego-inexistente")).isNull();
    }

    @Test
    void searchGameByName_returnsNull_whenServerError() throws Exception {
        storeServer.enqueue(new MockResponse().setResponseCode(500));

        assertThat(client.searchGameByName("half-life")).isNull();
    }

    @Test
    void getPlayerAchievements_mapsResponseAndPerformsRequest() throws Exception {
        apiServer.enqueue(new MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                .setBody(playerAchievementsFixture()));

        List<SteamAchievement> achievements = client.getPlayerAchievements(70L, "76561198000000000");

        assertThat(achievements).hasSize(1);
        SteamAchievement achievement = achievements.get(0);
        assertThat(achievement.apiname()).isEqualTo("ACH_BORN");
        assertThat(achievement.achieved()).isTrue();
        assertThat(achievement.name()).isEqualTo("El nacimiento");
        assertThat(achievement.description()).isEqualTo("Comienza la aventura.");
        assertThat(achievement.iconUrl()).isNull();

        RecordedRequest request = apiServer.takeRequest();
        assertThat(request.getPath()).startsWith("/ISteamUserStats/GetPlayerAchievements/v1?");
        assertThat(request.getPath()).contains("steamid=76561198000000000");
        assertThat(request.getPath()).contains("appid=70");
        assertThat(request.getPath()).contains("key=my-secret-key");
    }

    @Test
    void getPlayerAchievements_mapsAchievedZeroToFalse() throws Exception {
        apiServer.enqueue(new MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                .setBody(playerAchievementsFixtureWithZero()));

        List<SteamAchievement> achievements = client.getPlayerAchievements(70L, "76561198000000000");

        assertThat(achievements.get(0).achieved()).isFalse();
    }

    @Test
    void getPlayerAchievements_returnsEmpty_whenNoAchievements() throws Exception {
        apiServer.enqueue(new MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                .setBody("{\"playerstats\":{\"success\":true,\"achievements\":[]}}"));

        assertThat(client.getPlayerAchievements(70L, "76561198000000000")).isEmpty();
    }

    @Test
    void getPlayerAchievements_returnsEmpty_whenServerError() throws Exception {
        apiServer.enqueue(new MockResponse().setResponseCode(500));

        assertThat(client.getPlayerAchievements(70L, "76561198000000000")).isEmpty();
    }

    @Test
    void getGameSchema_mapsResponseAndPerformsRequest() throws Exception {
        apiServer.enqueue(new MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                .setBody(schemaFixture()));

        List<SteamAchievement> achievements = client.getGameSchema(70L);

        assertThat(achievements).hasSize(1);
        SteamAchievement achievement = achievements.get(0);
        assertThat(achievement.apiname()).isEqualTo("ACH_BORN");
        assertThat(achievement.achieved()).isFalse();
        assertThat(achievement.name()).isEqualTo("El nacimiento");
        assertThat(achievement.description()).isEqualTo("Comienza la aventura.");
        assertThat(achievement.iconUrl()).isEqualTo("https://cdn.akamai.steamstatic.com/.../icon.jpg");

        RecordedRequest request = apiServer.takeRequest();
        assertThat(request.getPath()).startsWith("/ISteamUserStats/GetSchemaForGame/v2?");
        assertThat(request.getPath()).contains("appid=70");
        assertThat(request.getPath()).contains("key=my-secret-key");
    }

    @Test
    void getGameSchema_returnsEmpty_whenNoAchievements() throws Exception {
        apiServer.enqueue(new MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                .setBody("{\"game\":{\"availableGameStats\":{\"achievements\":[],\"stats\":[]}}}"));

        assertThat(client.getGameSchema(70L)).isEmpty();
    }

    @Test
    void getGameSchema_returnsEmpty_whenServerError() throws Exception {
        apiServer.enqueue(new MockResponse().setResponseCode(500));

        assertThat(client.getGameSchema(70L)).isEmpty();
    }

    private String storesearchFixture() {
        return """
                {
                  "total": 1,
                  "items": [
                    {
                      "type": "app",
                      "name": "Half-Life",
                      "id": 70,
                      "price": {"final": 0, "initial": 0}
                    }
                  ]
                }
                """;
    }

    private String playerAchievementsFixture() {
        return """
                {
                  "playerstats": {
                    "success": true,
                    "steamID": "76561198000000000",
                    "gameName": "Half-Life",
                    "achievements": [
                      {
                        "apiname": "ACH_BORN",
                        "achieved": 1,
                        "unlocktime": 0,
                        "name": "El nacimiento",
                        "description": "Comienza la aventura."
                      }
                    ]
                  }
                }
                """;
    }

    private String playerAchievementsFixtureWithZero() {
        return """
                {
                  "playerstats": {
                    "success": true,
                    "steamID": "76561198000000000",
                    "gameName": "Half-Life",
                    "achievements": [
                      {
                        "apiname": "ACH_BORN",
                        "achieved": 0,
                        "unlocktime": 0,
                        "name": "El nacimiento",
                        "description": "Comienza la aventura."
                      }
                    ]
                  }
                }
                """;
    }

    private String schemaFixture() {
        return """
                {
                  "game": {
                    "gameName": "Half-Life",
                    "gameVersion": "1",
                    "availableGameStats": {
                      "achievements": [
                        {
                          "name": "ACH_BORN",
                          "defaultvalue": 0,
                          "displayName": "El nacimiento",
                          "hidden": 0,
                          "description": "Comienza la aventura.",
                          "icon": "https://cdn.akamai.steamstatic.com/.../icon.jpg",
                          "icongray": "https://cdn.akamai.steamstatic.com/.../icongray.jpg"
                        }
                      ],
                      "stats": []
                    }
                  }
                }
                """;
    }
}