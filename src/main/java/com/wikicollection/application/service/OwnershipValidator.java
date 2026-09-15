package com.wikicollection.application.service;

import com.wikicollection.application.exception.ForbiddenException;
import com.wikicollection.domain.port.out.UserRepository;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class OwnershipValidator {

    private final UserRepository userRepository;
    private final String adminUsername;

    public OwnershipValidator(UserRepository userRepository,
                              @Value("${app.admin.username:admin}") String adminUsername) {
        this.userRepository = userRepository;
        this.adminUsername = adminUsername;
    }

    public void validateOwner(String resourceOwnerId, String currentUserId) {
        if (resourceOwnerId != null && resourceOwnerId.equals(currentUserId)) {
            return;
        }
        if (isAdmin(currentUserId)) {
            return;
        }
        throw new ForbiddenException("No tienes permiso sobre este recurso");
    }

    public boolean isAdmin(String userId) {
        if (userId == null || adminUsername == null) {
            return false;
        }
        return userRepository.findById(userId)
                .map(user -> adminUsername.equals(user.getUsername()))
                .orElse(false);
    }
}
