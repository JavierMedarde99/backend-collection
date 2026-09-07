package com.wikicollection.infrastructure.adapter.in.web.dto;

import java.util.List;

public record AchievementsResponse(
        List<GameAchievementResponse> achievements,
        int totalAchievements,
        int totalAchieved,
        double percentage) {
}