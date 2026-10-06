package com.wikicollection.domain.model;

/** En qué punto va un trabajo de importación (#353). */
public enum DeckImportPhase {

    /** Leyendo el archivo y separando las cartas del sideboard. */
    PARSING,

    /** Buscando cada nombre en Scryfall. */
    RESOLVING,

    /** Escribiendo el mazo. */
    SAVING,

    /** Nada más que hacer. */
    DONE
}