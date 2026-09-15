package com.wikicollection.domain.port.in;

import com.wikicollection.domain.model.AuthTokens;
import com.wikicollection.domain.model.User;

public interface UserUseCase {

    AuthTokens register(String username, String email, String password, String displayName);

    AuthTokens login(String username, String password);

    AuthTokens refresh(String refreshToken);

    User getById(String userId);
}
