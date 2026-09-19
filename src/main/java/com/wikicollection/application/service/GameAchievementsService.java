package com.wikicollection.application.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.wikicollection.application.exception.GameNotFoundException;
import com.wikicollection.domain.model.AchievementsSummary;
import com.wikicollection.domain.model.Game;
import com.wikicollection.domain.model.SteamAchievement;
import com.wikicollection.domain.model.User;
import com.wikicollection.domain.port.in.GameAchievementsUseCase;
import com.wikicollection.domain.port.out.GameRepository;
import com.wikicollection.domain.port.out.SteamCatalogueClient;
import com.wikicollection.domain.port.out.UserRepository;

import org.springframework.stereotype.Service;

@Service
public class GameAchievementsService implements GameAchievementsUseCase {

    private final GameRepository gameRepository;
    private final SteamCatalogueClient steamCatalogueClient;
    private final UserRepository userRepository;

    public GameAchievementsService(GameRepository gameRepository, SteamCatalogueClient steamCatalogueClient,
                                   UserRepository userRepository) {
        this.gameRepository = gameRepository;
        this.steamCatalogueClient = steamCatalogueClient;
        this.userRepository = userRepository;
    }

    @Override
    public AchievementsSummary getAchievements(String gameId, String steamId) {
        Game game = gameRepository.findById(gameId)
                .orElseThrow(() -> new GameNotFoundException("Juego no encontrado con id: " + gameId));

        String effectiveSteamId = (steamId != null && !steamId.isBlank())
                ? steamId
                : ownerSteamId(game);
        if (effectiveSteamId == null || effectiveSteamId.isBlank()) {
            throw new IllegalArgumentException("El parámetro steamId es obligatorio");
        }

        String steamAppId = game.getSteamAppId();
        if (steamAppId == null || steamAppId.isBlank()) {
            throw new IllegalArgumentException("El juego no está vinculado a Steam");
        }

        Long appId = Long.valueOf(steamAppId);
        List<SteamAchievement> schema = steamCatalogueClient.getGameSchema(appId);
        List<SteamAchievement> combined;
        if (schema.isEmpty()) {
            combined = steamCatalogueClient.getPlayerAchievements(appId, effectiveSteamId);
        } else {
            Map<String, Boolean> achievedByApiname = new HashMap<>();
            for (SteamAchievement playerAchievement : steamCatalogueClient.getPlayerAchievements(appId, effectiveSteamId)) {
                achievedByApiname.put(playerAchievement.apiname(), playerAchievement.achieved());
            }

            combined = new ArrayList<>();
            for (SteamAchievement schemaAchievement : schema) {
                boolean achieved = achievedByApiname.getOrDefault(schemaAchievement.apiname(), false);
                combined.add(new SteamAchievement(
                        schemaAchievement.apiname(),
                        achieved,
                        schemaAchievement.name(),
                        schemaAchievement.description(),
                        schemaAchievement.iconUrl()));
            }
        }

        int totalAchievements = combined.size();
        int totalAchieved = (int) combined.stream().filter(SteamAchievement::achieved).count();
        double percentage = totalAchievements == 0 ? 0.0 : Math.round(totalAchieved * 1000.0 / totalAchievements) / 10.0;
        return new AchievementsSummary(combined, totalAchievements, totalAchieved, percentage);
    }

    private String ownerSteamId(Game game) {
        if (game.getOwnerId() == null || game.getOwnerId().isBlank()) {
            return null;
        }
        return userRepository.findById(game.getOwnerId())
                .map(User::getSteamId)
                .orElse(null);
    }
}