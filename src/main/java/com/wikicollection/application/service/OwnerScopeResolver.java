package com.wikicollection.application.service;

import java.util.ArrayList;
import java.util.List;

import com.wikicollection.application.exception.UnauthenticatedException;
import com.wikicollection.domain.model.CollectionType;
import com.wikicollection.domain.port.in.UserPreferencesUseCase;

import org.springframework.stereotype.Component;

@Component
public class OwnerScopeResolver {

    public record Scope(String ownerId, List<String> excludeOwnerIds) {
    }

    private final UserPreferencesUseCase preferencesUseCase;

    public OwnerScopeResolver(UserPreferencesUseCase preferencesUseCase) {
        this.preferencesUseCase = preferencesUseCase;
    }

    public Scope resolve(CollectionType type, String owner, String viewerId) {
        String mode = owner == null ? "mine" : owner;
        return switch (mode) {
            case "mine" -> {
                requireAuthenticated(viewerId);
                yield new Scope(viewerId, List.of());
            }
            case "other" -> {
                List<String> excluded = new ArrayList<>(
                        preferencesUseCase.getUserIdsWithPrivateCollection(type));
                if (viewerId != null) {
                    excluded.add(viewerId);
                }
                yield new Scope(null, excluded);
            }
            case "all" -> {
                requireAuthenticated(viewerId);
                List<String> excluded = new ArrayList<>(
                        preferencesUseCase.getUserIdsWithPrivateCollection(type));
                excluded.remove(viewerId);
                yield new Scope(null, excluded);
            }
            default -> throw new IllegalArgumentException(
                    "Parámetro owner inválido: " + owner + " (mine|other|all)");
        };
    }

    private void requireAuthenticated(String viewerId) {
        if (viewerId == null) {
            throw new UnauthenticatedException("Este modo requiere autenticación");
        }
    }
}
