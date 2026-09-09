package com.wikicollection.domain.model;

public record MagicCardSearchCriteria(
        String name,
        String rarity,
        String color,
        String type,
        Double convertedManaCost) {

    public MagicCardSearchCriteria(String name) {
        this(name, null, null, null, null);
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

    public boolean hasConvertedManaCost() {
        return convertedManaCost != null;
    }
}
