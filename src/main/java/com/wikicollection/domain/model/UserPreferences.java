package com.wikicollection.domain.model;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserPreferences {

    private String id;
    private String userId;
    private Map<String, Boolean> activeCollections;
    private Map<String, CollectionVisibility> collectionVisibility;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static UserPreferences defaults(String userId) {
        LocalDateTime now = LocalDateTime.now();
        Map<String, Boolean> active = Arrays.stream(CollectionType.values())
                .collect(Collectors.toMap(CollectionType::getKey, type -> true));
        Map<String, CollectionVisibility> visibility = Arrays.stream(CollectionType.values())
                .collect(Collectors.toMap(CollectionType::getKey, type -> CollectionVisibility.PUBLIC));
        return UserPreferences.builder()
                .userId(userId)
                .activeCollections(active)
                .collectionVisibility(visibility)
                .createdAt(now)
                .updatedAt(now)
                .build();
    }
}
