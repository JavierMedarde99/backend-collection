package com.wikicollection.domain.model;

/** Por qué una línea del archivo no acabó en el mazo (#353). */
public enum UnresolvedReason {

    /** Scryfall no tiene ninguna carta con ese nombre. */
    NOT_FOUND,

    /** Varias cartas podrían encajar y no hay forma de elegir sin el usuario. */
    AMBIGUOUS,

    /** Scryfall falló o no respondió. No significa que la carta no exista. */
    UPSTREAM_ERROR
}