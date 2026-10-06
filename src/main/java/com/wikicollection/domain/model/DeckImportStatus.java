package com.wikicollection.domain.model;

/** Estado de un trabajo de importación de mazos (#353). */
public enum DeckImportStatus {

    /** Aceptado y en cola, aún sin empezar. */
    PENDING,

    /** El worker lo está procesando. */
    RUNNING,

    /** Terminó: el mazo está guardado, aunque alguna carta quedara sin resolver. */
    COMPLETED,

    /** Terminó con error: el mazo no se ha tocado. */
    FAILED
}