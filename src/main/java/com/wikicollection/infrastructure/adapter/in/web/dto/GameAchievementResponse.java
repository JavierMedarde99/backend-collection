package com.wikicollection.infrastructure.adapter.in.web.dto;

public record GameAchievementResponse(
        String name,
        String description,
        boolean achieved,
        String iconUrl) {
}