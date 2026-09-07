package com.wikicollection.domain.model;

public record SteamAchievement(
        String apiname,
        boolean achieved,
        String name,
        String description,
        String iconUrl) {
}