package com.wikicollection.application.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.wikicollection.application.exception.GameNotFoundException;
import com.wikicollection.domain.model.Game;
import com.wikicollection.domain.model.SteamAchievement;
import com.wikicollection.domain.port.in.GameAchievementsUseCase;
import com.wikicollection.domain.port.out.GameRepository;
import com.wikicollection.domain.port.out.SteamCatalogueClient;

import org.springframework.stereotype.Service;

@Service
public class GameAchievementsService implements GameAchievementsUseCase {

    private final GameRepository gameRepository;
    private final SteamCatalogueClient steamCatalogueClient;

    public GameAchievementsService(GameRepository gameRepository, SteamCatalogueClient steamCatalogueClient) {
        this.gameRepository = gameRepository;
        this.steamCatalogueClient = steamCatalogueClient;
    }

    @Override
    public List<SteamAchievement> getAchievements(String gameId, String steamId) {
        if (steamId == null || steamId.isBlank()) {
            throw new IllegalArgumentException("El parámetro steamId es obligatorio");
        }

        Game game = gameRepository.findById(gameId)
                .orElseThrow(() -> new GameNotFoundException("Juego no encontrado con id: " + gameId));

        String steamAppId = game.getSteamAppId();
        if (steamAppId == null || steamAppId.isBlank()) {
            throw new IllegalArgumentException("El juego no está vinculado a Steam");
        }

        Long appId = Long.valueOf(steamAppId);
        List<SteamAchievement> schema = steamCatalogueClient.getGameSchema(appId);
        if (schema.isEmpty()) {
            return steamCatalogueClient.getPlayerAchievements(appId, steamId);
        }

        Map<String, Boolean> achievedByApiname = new HashMap<>();
        for (SteamAchievement playerAchievement : steamCatalogueClient.getPlayerAchievements(appId, steamId)) {
            achievedByApiname.put(playerAchievement.apiname(), playerAchievement.achieved());
        }

        List<SteamAchievement> combined = new ArrayList<>();
        for (SteamAchievement schemaAchievement : schema) {
            boolean achieved = achievedByApiname.getOrDefault(schemaAchievement.apiname(), false);
            combined.add(new SteamAchievement(
                    schemaAchievement.apiname(),
                    achieved,
                    schemaAchievement.name(),
                    schemaAchievement.description(),
                    schemaAchievement.iconUrl()));
        }
        return combined;
    }
}