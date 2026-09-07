package com.wikicollection.domain.model;

import java.util.List;

public record AchievementsSummary(
        List<SteamAchievement> achievements,
        int totalAchievements,
        int totalAchieved,
        double percentage) {
}