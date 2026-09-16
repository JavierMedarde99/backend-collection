package com.wikicollection.infrastructure.adapter.out.persistence;

import java.util.Map;
import java.util.stream.Collectors;

import com.wikicollection.domain.model.CollectionVisibility;
import com.wikicollection.domain.model.UserPreferences;

import org.springframework.stereotype.Component;

@Component
public class UserPreferencesEntityMapper {

    public UserPreferencesEntity toEntity(UserPreferences prefs) {
        if (prefs == null) {
            return null;
        }
        return UserPreferencesEntity.builder()
                .id(prefs.getId())
                .userId(prefs.getUserId())
                .activeCollections(prefs.getActiveCollections())
                .collectionVisibility(prefs.getCollectionVisibility() == null ? null
                        : prefs.getCollectionVisibility().entrySet().stream()
                                .collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().name())))
                .createdAt(prefs.getCreatedAt())
                .updatedAt(prefs.getUpdatedAt())
                .build();
    }

    public UserPreferences toDomain(UserPreferencesEntity entity) {
        if (entity == null) {
            return null;
        }
        return UserPreferences.builder()
                .id(entity.getId())
                .userId(entity.getUserId())
                .activeCollections(entity.getActiveCollections())
                .collectionVisibility(entity.getCollectionVisibility() == null ? null
                        : entity.getCollectionVisibility().entrySet().stream()
                                .collect(Collectors.toMap(Map.Entry::getKey,
                                        e -> CollectionVisibility.valueOf(e.getValue()))))
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }
}
