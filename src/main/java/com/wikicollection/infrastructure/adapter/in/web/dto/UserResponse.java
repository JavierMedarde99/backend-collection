package com.wikicollection.infrastructure.adapter.in.web.dto;

import java.time.LocalDateTime;

import com.wikicollection.domain.model.User;

public record UserResponse(
        String id,
        String username,
        String displayName,
        String avatarUrl,
        String bio,
        String steamId,
        LocalDateTime createdAt) {

    public static UserResponse from(User user) {
        if (user == null) {
            return null;
        }
        return new UserResponse(
                user.getId(), user.getUsername(),
                user.getDisplayName(), user.getAvatarUrl(), user.getBio(),
                user.getSteamId(),
                user.getCreatedAt());
    }
}
