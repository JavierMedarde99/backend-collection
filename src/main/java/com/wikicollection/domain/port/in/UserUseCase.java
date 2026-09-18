package com.wikicollection.domain.port.in;

import com.wikicollection.domain.model.AuthSession;
import com.wikicollection.domain.model.User;

public interface UserUseCase {

    AuthSession register(String username, String email, String password, String displayName);

    AuthSession login(String username, String password);

    AuthSession refresh(String refreshToken);

    User getById(String userId);

    User updateProfile(String userId, String displayName, String avatarUrl, String bio);

    void deleteAccount(String userId);
}
