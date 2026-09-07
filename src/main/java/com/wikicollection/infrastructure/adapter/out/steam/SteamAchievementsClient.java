package com.wikicollection.infrastructure.adapter.out.steam;

import java.util.List;

import com.wikicollection.domain.model.SteamAchievement;
import com.wikicollection.domain.port.out.SteamCatalogueClient;

import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

@Slf4j
@Component("steamAchievementsClient")
public class SteamAchievementsClient implements SteamCatalogueClient {

    private static final String STORE_SEARCH_PATH = "/storesearch";
    private static final String PLAYER_ACHIEVEMENTS_PATH = "/ISteamUserStats/GetPlayerAchievements/v1";
    private static final String GAME_SCHEMA_PATH = "/ISteamUserStats/GetSchemaForGame/v2";

    private final RestClient steamRestClient;
    private final RestClient steamStoreRestClient;
    private final String apiKey;

    public SteamAchievementsClient(
            @Qualifier("steamRestClient") RestClient steamRestClient,
            @Qualifier("steamStoreRestClient") RestClient steamStoreRestClient,
            @Value("${steam.api-key:}") String apiKey) {
        this.steamRestClient = steamRestClient;
        this.steamStoreRestClient = steamStoreRestClient;
        this.apiKey = apiKey;
    }

    @Override
    public Long searchGameByName(String name) {
        try {
            StoreSearchResponse response = steamStoreRestClient.get()
                    .uri(uriBuilder -> uriBuilder.path(STORE_SEARCH_PATH)
                            .queryParam("term", name)
                            .queryParam("l", "spanish")
                            .queryParam("cc", "ES")
                            .build())
                    .retrieve()
                    .body(StoreSearchResponse.class);

            if (response == null || response.items() == null || response.items().isEmpty()) {
                return null;
            }
            return response.items().get(0).id();
        } catch (RestClientResponseException e) {
            log.warn("Steam Store devolvió error {}: {}", e.getStatusCode(), e.getResponseBodyAsString());
            return null;
        } catch (ResourceAccessException e) {
            log.warn("Steam Store no disponible: {}", e.getMessage());
            return null;
        }
    }

    @Override
    public List<SteamAchievement> getPlayerAchievements(Long appId, String steamId) {
        try {
            PlayerAchievementsResponse response = steamRestClient.get()
                    .uri(uriBuilder -> uriBuilder.path(PLAYER_ACHIEVEMENTS_PATH)
                            .queryParam("steamid", steamId)
                            .queryParam("appid", appId)
                            .queryParam("key", apiKey)
                            .build())
                    .retrieve()
                    .body(PlayerAchievementsResponse.class);

            if (response == null || response.playerstats() == null
                    || response.playerstats().achievements() == null) {
                return List.of();
            }
            return response.playerstats().achievements().stream()
                    .map(this::toPlayerAchievement)
                    .toList();
        } catch (RestClientResponseException e) {
            log.warn("Steam Web API devolvió error {}: {}", e.getStatusCode(), e.getResponseBodyAsString());
            return List.of();
        } catch (ResourceAccessException e) {
            log.warn("Steam Web API no disponible: {}", e.getMessage());
            return List.of();
        }
    }

    @Override
    public List<SteamAchievement> getGameSchema(Long appId) {
        try {
            GameSchemaResponse response = steamRestClient.get()
                    .uri(uriBuilder -> uriBuilder.path(GAME_SCHEMA_PATH)
                            .queryParam("appid", appId)
                            .queryParam("key", apiKey)
                            .build())
                    .retrieve()
                    .body(GameSchemaResponse.class);

            if (response == null || response.game() == null
                    || response.game().availableGameStats() == null
                    || response.game().availableGameStats().achievements() == null) {
                return List.of();
            }
            return response.game().availableGameStats().achievements().stream()
                    .map(this::toSchemaAchievement)
                    .toList();
        } catch (RestClientResponseException e) {
            log.warn("Steam Web API devolvió error {}: {}", e.getStatusCode(), e.getResponseBodyAsString());
            return List.of();
        } catch (ResourceAccessException e) {
            log.warn("Steam Web API no disponible: {}", e.getMessage());
            return List.of();
        }
    }

    private SteamAchievement toPlayerAchievement(PlayerAchievement achievement) {
        return new SteamAchievement(
                achievement.apiname(),
                achievement.achieved() != null && achievement.achieved() == 1,
                achievement.name(),
                achievement.description(),
                null);
    }

    private SteamAchievement toSchemaAchievement(SchemaAchievement achievement) {
        return new SteamAchievement(
                achievement.name(),
                false,
                achievement.displayName(),
                achievement.description(),
                achievement.icon());
    }

    record StoreSearchResponse(List<StoreItem> items) {
    }

    record StoreItem(Long id) {
    }

    record PlayerAchievementsResponse(PlayerStats playerstats) {
    }

    record PlayerStats(List<PlayerAchievement> achievements) {
    }

    record PlayerAchievement(String apiname, Integer achieved, String name, String description) {
    }

    record GameSchemaResponse(SchemaGame game) {
    }

    record SchemaGame(SchemaStats availableGameStats) {
    }

    record SchemaStats(List<SchemaAchievement> achievements) {
    }

    record SchemaAchievement(String name, String displayName, String description, String icon) {
    }
}