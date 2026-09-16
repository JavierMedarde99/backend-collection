package com.wikicollection.infrastructure.adapter.in.web;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import com.wikicollection.application.service.UserPrincipal;

/**
 * Decide si el visor actual puede ver los campos privados (notas, valoraciones,
 * comentarios) de un recurso: solo el dueño o un admin. Réplica la resolución
 * de identidad de {@code CurrentUserHandlerMethodArgumentResolver}.
 */
@Component
public class ResponseVisibility {

    public boolean canSeePrivate(String ownerId) {
        String viewerId = currentUserId();
        if (viewerId != null && viewerId.equals(ownerId)) {
            return true;
        }
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication != null && authentication.getAuthorities().stream()
                .anyMatch(authority -> "ROLE_ADMIN".equals(authority.getAuthority()));
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
