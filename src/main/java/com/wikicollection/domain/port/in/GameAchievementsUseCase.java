package com.wikicollection.domain.port.in;

import com.wikicollection.domain.model.AchievementsSummary;

public interface GameAchievementsUseCase {

    AchievementsSummary getAchievements(String gameId, String steamId);
}