package com.wikicollection.infrastructure.adapter.in.web;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import com.wikicollection.application.service.UserPrincipal;
import com.wikicollection.domain.model.CollectionType;
import com.wikicollection.domain.port.in.UserPreferencesUseCase;

/**
 * Decide si el visor actual puede ver los campos privados (notas, valoraciones,
 * comentarios) de un recurso: el dueño, un admin, o cualquiera cuando la
 * colección del dueño está en PUBLIC. Réplica la resolución
 * de identidad de {@code CurrentUserHandlerMethodArgumentResolver}.
 */
@Component
public class ResponseVisibility {

    private final UserPreferencesUseCase preferencesUseCase;

    public ResponseVisibility(UserPreferencesUseCase preferencesUseCase) {
        this.preferencesUseCase = preferencesUseCase;
    }

    public boolean canSeePrivate(String ownerId, CollectionType type) {
        String viewerId = currentUserId();
        if (viewerId != null && viewerId.equals(ownerId)) {
            return true;
        }
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getAuthorities().stream()
                .anyMatch(authority -> "ROLE_ADMIN".equals(authority.getAuthority()))) {
            return true;
        }
        return !isPrivateCollection(ownerId, type);
    }

    private boolean isPrivateCollection(String ownerId, CollectionType type) {
        var privateOwners = preferencesUseCase.getUserIdsWithPrivateCollection(type);
        return privateOwners != null && privateOwners.contains(ownerId);
    }

    private static String currentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()
                || authentication instanceof org.springframework.security.authentication.AnonymousAuthenticationToken) {
            return null;
        }
        Object principal = authentication.getPrincipal();
        if (principal instanceof UserPrincipal userPrincipal) {
            return userPrincipal.getId();
        }
        if (principal instanceof UserDetails details) {
            return details.getUsername();
        }
        return principal instanceof String name ? name : null;
    }
}
