package com.wikicollection.domain.model;

import java.util.List;

public record GameSearchCriteria(
        String name,
        String platform,
        GameStatus status,
        java.util.List<String> genres,
        String ownerId,
        List<String> excludeOwnerIds,
        GameTypeFilter type) {

    public GameSearchCriteria {
        // «sin filtro» es ALL: el resto de la capa no tiene que tratar null.
        type = type == null ? GameTypeFilter.ALL : type;
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

    public boolean isBaseOnly() {
        return type == GameTypeFilter.BASE;
    }

    public boolean isDlcOnly() {
        return type == GameTypeFilter.DLC;
    }
}
