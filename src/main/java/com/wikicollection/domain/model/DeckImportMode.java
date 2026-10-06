package com.wikicollection.domain.model;

/** Qué hacer con las cartas que ya tenía el mazo (#353). */
public enum DeckImportMode {

    /** El mazo se queda exactamente con las cartas del archivo. Es el modo por defecto. */
    REPLACE,

    /** Las cantidades del archivo se suman a las que ya había. */
    MERGE
}