package com.wikicollection.application.service;

import com.wikicollection.domain.model.UserOwned;
import com.wikicollection.domain.port.out.UserRepository;

import org.springframework.stereotype.Component;

@Component
public class OwnerResolver {

    private final UserRepository userRepository;

    public OwnerResolver(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public UserOwned resolveOwner(String ownerId) {
        var user = userRepository.findById(ownerId);
        String ownerName = user
                .map(u -> u.getDisplayName() != null && !u.getDisplayName().isBlank()
                        ? u.getDisplayName()
                        : u.getUsername())
                .orElse(ownerId);
        String username = user.map(com.wikicollection.domain.model.User::getUsername).orElse(ownerId);
        return UserOwned.builder().ownerId(ownerId).ownerName(ownerName).username(username).build();
    }
}
