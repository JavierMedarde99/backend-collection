package com.wikicollection.domain.model;

import java.util.List;

public record BoardGameSearchCriteria(
        String name,
        BoardGameStatus status,
        java.util.List<String> genres,
        String ownerId,
        List<String> excludeOwnerIds,
        BoardGameTypeFilter type) {

    public BoardGameSearchCriteria {
        // «sin filtro» es ALL: el resto de la capa no tiene que tratar null.
        type = type == null ? BoardGameTypeFilter.ALL : type;
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
        return type == BoardGameTypeFilter.BASE;
    }

    public boolean isExpansionOnly() {
        return type == BoardGameTypeFilter.EXPANSION;
    }
}
