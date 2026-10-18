package com.wikicollection.domain.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * Tipo de un juego de mesa según BoardGameGeek. El vocabulario es el de BGG
 * porque es lo que llega del XML: un registro es una entrada del catálogo de
 * BGG o una expansión de otra entrada.
 */
public enum BoardGameType {

    BOARDGAME("boardgame"),
    BOARDGAME_EXPANSION("boardgameexpansion");

    private final String key;

    BoardGameType(String key) {
        this.key = key;
    }

    @JsonValue
    public String getKey() {
        return key;
    }

    @JsonCreator
    public static BoardGameType fromKey(String key) {
        if (key == null) {
            return null;
        }
        String normalized = key.trim();
        for (BoardGameType type : values()) {
            if (type.key.equalsIgnoreCase(normalized)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Tipo de juego de mesa desconocido: " + key);
    }
}
