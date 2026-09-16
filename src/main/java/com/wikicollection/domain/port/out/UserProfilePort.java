package com.wikicollection.domain.port.out;

import java.util.Optional;

import com.wikicollection.domain.model.User;

public interface UserProfilePort {

    Optional<User> findByUsername(String username);
}
