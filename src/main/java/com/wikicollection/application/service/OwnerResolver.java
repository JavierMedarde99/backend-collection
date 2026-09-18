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
        String ownerName = userRepository.findById(ownerId)
                .map(user -> user.getDisplayName() != null && !user.getDisplayName().isBlank()
                        ? user.getDisplayName()
                        : user.getUsername())
                .orElse(ownerId);
        return UserOwned.builder().ownerId(ownerId).ownerName(ownerName).build();
    }
}
