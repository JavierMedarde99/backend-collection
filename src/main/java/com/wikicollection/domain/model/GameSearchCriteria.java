package com.wikicollection.domain.model;

import java.util.List;

public record GameSearchCriteria(
        String name,
        String platform,
        GameStatus status,
        java.util.List<String> genres,
        String ownerId,
        List<String> excludeOwnerIds,
        boolean groupByGame) {

    /**
     * Búsqueda plana (la de siempre). {@code groupByGame} forma parte de la criteria —y por
     * tanto de la clave de caché— para que una llamada agrupada y otra plana no compartan
     * entrada en {@code gameList}.
     */
    public GameSearchCriteria(String name, String platform, GameStatus status, java.util.List<String> genres,
                              String ownerId, List<String> excludeOwnerIds) {
        this(name, platform, status, genres, ownerId, excludeOwnerIds, false);
    }

    public boolean hasName() {
        return name != null && !name.isBlank();
    }

    public boolean hasGenres() {
        return genres != null && genres.stream().anyMatch(g -> g != null && !g.isBlank());
    }

    public java.util.List<String> effectiveGenres() {
        if (genres == null) {
            return java.util.List.of();
        }
        return genres.stream().filter(g -> g != null && !g.isBlank()).toList();
    }

    public boolean hasOwnerId() {
        return ownerId != null && !ownerId.isBlank();
    }

    public boolean hasExcludeOwnerIds() {
        return excludeOwnerIds != null && !excludeOwnerIds.isEmpty();
    }
}
