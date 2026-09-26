package com.wikicollection.application.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import com.wikicollection.application.exception.UserNotFoundException;
import com.wikicollection.domain.model.CollectionType;
import com.wikicollection.domain.model.CollectionVisibility;
import com.wikicollection.domain.model.UserPreferences;
import com.wikicollection.domain.port.in.UserPreferencesUseCase;
import com.wikicollection.domain.port.out.UserPreferencesRepository;
import com.wikicollection.domain.port.out.UserRepository;

import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

@Service
public class UserPreferencesService implements UserPreferencesUseCase {

    private final UserPreferencesRepository preferencesRepository;
    private final UserRepository userRepository;

    public UserPreferencesService(UserPreferencesRepository preferencesRepository,
                                  UserRepository userRepository) {
        this.preferencesRepository = preferencesRepository;
        this.userRepository = userRepository;
    }

    @Override
    @Cacheable(cacheNames = "userPreferencesDetail", key = "#userId")
    public UserPreferences getPreferences(String userId) {
        requireUser(userId);
        return preferencesRepository.findByUserId(userId)
                .orElseGet(() -> preferencesRepository.save(UserPreferences.defaults(userId)));
    }

    @Override
    public UserPreferences updatePreferences(String userId, Map<String, Boolean> activeCollections,
                                             Map<String, CollectionVisibility> visibility) {
        requireUser(userId);
        validateKeys(activeCollections.keySet());
        validateKeys(visibility.keySet());
        UserPreferences prefs = loadOrCreate(userId);
        prefs.setActiveCollections(activeCollections);
        prefs.setCollectionVisibility(visibility);
        prefs.setUpdatedAt(LocalDateTime.now());
        return preferencesRepository.save(prefs);
    }

    @Override
    public UserPreferences setActiveCollections(String userId, Map<String, Boolean> collections) {
        requireUser(userId);
        validateKeys(collections.keySet());
        UserPreferences prefs = loadOrCreate(userId);
        prefs.getActiveCollections().putAll(collections);
        prefs.setUpdatedAt(LocalDateTime.now());
        return preferencesRepository.save(prefs);
    }

    @Override
    public UserPreferences setCollectionVisibility(String userId, Map<String, CollectionVisibility> visibility) {
        requireUser(userId);
        validateKeys(visibility.keySet());
        UserPreferences prefs = loadOrCreate(userId);
        prefs.getCollectionVisibility().putAll(visibility);
        prefs.setUpdatedAt(LocalDateTime.now());
        return preferencesRepository.save(prefs);
    }

    @Override
    @Cacheable(cacheNames = "userActiveCollections", key = "#userId")
    public List<CollectionType> getActiveCollections(String userId) {
        return prefsOrDefaults(userId).getActiveCollections().entrySet().stream()
                .filter(Map.Entry::getValue)
                .map(entry -> CollectionType.fromKey(entry.getKey()))
                .toList();
    }

    @Override
    @Cacheable(cacheNames = "userPreferenceFlags", key = "#userId + ':' + #type")
    public boolean isCollectionActive(String userId, CollectionType type) {
        return prefsOrDefaults(userId).getActiveCollections().getOrDefault(type.getKey(), true);
    }

    @Override
    public boolean isCollectionPublic(String userId, CollectionType type) {
        return prefsOrDefaults(userId).getCollectionVisibility()
                .getOrDefault(type.getKey(), CollectionVisibility.PUBLIC) == CollectionVisibility.PUBLIC;
    }

    @Override
    public List<String> getUserIdsWithPrivateCollection(CollectionType type) {
        return preferencesRepository.findUserIdsWithPrivateCollection(type.getKey());
    }

    private void requireUser(String userId) {
        userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("Usuario no encontrado: " + userId));
    }

    private UserPreferences loadOrCreate(String userId) {
        return preferencesRepository.findByUserId(userId)
                .orElseGet(() -> UserPreferences.defaults(userId));
    }

    private UserPreferences prefsOrDefaults(String userId) {
        return preferencesRepository.findByUserId(userId)
                .orElseGet(() -> UserPreferences.defaults(userId));
    }

    private void validateKeys(Iterable<String> keys) {
        for (String key : keys) {
            CollectionType.fromKey(key);
        }
    }
}
