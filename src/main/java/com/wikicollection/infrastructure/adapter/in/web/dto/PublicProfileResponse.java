package com.wikicollection.infrastructure.adapter.in.web.dto;

import java.util.Map;

import com.wikicollection.domain.model.User;

public record PublicProfileResponse(
        String username,
        String displayName,
        String avatarUrl,
        String bio,
        Map<String, Long> publicCollectionCounts) {

    public static PublicProfileResponse from(User user, Map<String, Long> counts) {
        if (user == null) {
            return null;
        }
        return new PublicProfileResponse(
                user.getUsername(), user.getDisplayName(), user.getAvatarUrl(),
                user.getBio(), counts);
    }
}
