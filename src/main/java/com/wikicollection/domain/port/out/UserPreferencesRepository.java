package com.wikicollection.domain.port.out;

import java.util.List;
import java.util.Optional;

import com.wikicollection.domain.model.UserPreferences;

public interface UserPreferencesRepository {

    UserPreferences save(UserPreferences preferences);

    Optional<UserPreferences> findByUserId(String userId);

    boolean existsByUserId(String userId);

    List<String> findUserIdsWithPrivateCollection(String collectionKey);
}
