package com.wikicollection.domain.port.in;

import java.util.List;

import com.wikicollection.domain.model.SteamAchievement;

public interface GameAchievementsUseCase {

    List<SteamAchievement> getAchievements(String gameId, String steamId);
}