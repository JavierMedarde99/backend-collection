package com.wikicollection.infrastructure.adapter.in.web.dto;

import java.time.LocalDateTime;
import java.util.Map;

import com.wikicollection.domain.model.CollectionVisibility;
import com.wikicollection.domain.model.UserPreferences;

public record UserPreferencesResponse(
        String id,
        String userId,
        Map<String, Boolean> activeCollections,
        Map<String, CollectionVisibility> collectionVisibility,
        LocalDateTime createdAt,
        LocalDateTime updatedAt) {

    public static UserPreferencesResponse from(UserPreferences prefs) {
        if (prefs == null) {
            return null;
        }
        return new UserPreferencesResponse(
                prefs.getId(), prefs.getUserId(), prefs.getActiveCollections(),
                prefs.getCollectionVisibility(), prefs.getCreatedAt(), prefs.getUpdatedAt());
    }
}
