package com.wikicollection.domain.port.out;

import java.util.List;

import com.wikicollection.domain.model.SteamAchievement;

public interface SteamCatalogueClient {

    Long searchGameByName(String name);

    List<SteamAchievement> getPlayerAchievements(Long appId, String steamId);

    List<SteamAchievement> getGameSchema(Long appId);
}