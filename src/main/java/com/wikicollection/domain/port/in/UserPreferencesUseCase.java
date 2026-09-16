package com.wikicollection.domain.port.in;

import java.util.List;
import java.util.Map;

import com.wikicollection.domain.model.CollectionType;
import com.wikicollection.domain.model.CollectionVisibility;
import com.wikicollection.domain.model.UserPreferences;

public interface UserPreferencesUseCase {

    UserPreferences getPreferences(String userId);

    UserPreferences updatePreferences(String userId, Map<String, Boolean> activeCollections,
                                      Map<String, CollectionVisibility> visibility);

    UserPreferences setActiveCollections(String userId, Map<String, Boolean> collections);

    UserPreferences setCollectionVisibility(String userId, Map<String, CollectionVisibility> visibility);

    List<CollectionType> getActiveCollections(String userId);

    boolean isCollectionActive(String userId, CollectionType type);

    boolean isCollectionPublic(String userId, CollectionType type);

    List<String> getUserIdsWithPrivateCollection(CollectionType type);
}
