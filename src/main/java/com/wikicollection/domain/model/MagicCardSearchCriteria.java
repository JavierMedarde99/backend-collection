package com.wikicollection.domain.model;

import java.util.List;

public record MagicCardSearchCriteria(
        String name,
        String rarity,
        String color,
        String type,
        String ownerId,
        List<String> excludeOwnerIds) {

    public MagicCardSearchCriteria(String name) {
        this(name, null, null, null, null, null);
    }

    public boolean hasName() {
        return name != null && !name.isBlank();
    }

    public boolean hasRarity() {
        return rarity != null && !rarity.isBlank();
    }

    public boolean hasColor() {
        return color != null && !color.isBlank();
    }

    public boolean hasType() {
        return type != null && !type.isBlank();
    }

    public boolean hasOwnerId() {
        return ownerId != null && !ownerId.isBlank();
    }

    public boolean hasExcludeOwnerIds() {
        return excludeOwnerIds != null && !excludeOwnerIds.isEmpty();
    }
}
