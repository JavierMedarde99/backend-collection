package com.wikicollection.infrastructure.adapter.in.web.dto;

import com.wikicollection.domain.model.SteamAchievement;

import org.springframework.stereotype.Component;

@Component
public class GameAchievementMapper {

    public GameAchievementResponse toResponse(SteamAchievement achievement) {
        if (achievement == null) {
            return null;
        }
        return new GameAchievementResponse(
                achievement.name(),
                achievement.description(),
                achievement.achieved(),
                achievement.iconUrl());
    }
}