package com.wikicollection.domain.model;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum CollectionType {
    BOOKS("books"),
    GAMES("games"),
    BOARDGAMES("boardgames"),
    MAGIC("magic"),
    DECKS("decks"),
    MOVIESHOWS("movieshows");

    private final String key;

    public static CollectionType fromKey(String key) {
        for (CollectionType type : values()) {
            if (type.key.equals(key)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Tipo de colección desconocido: " + key);
    }
}
