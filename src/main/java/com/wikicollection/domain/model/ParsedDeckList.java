package com.wikicollection.domain.model;

import java.util.List;

/**
 * Resultado de parsear un archivo de lista de mazo.
 *
 * @param entries         cartas encontradas, en el orden del archivo
 * @param sideboardIgnored líneas de la zona SIDEBOARD descartadas (el sideboard no se importa)
 */
public record ParsedDeckList(List<DeckListEntry> entries, int sideboardIgnored) {

    /** Copia defensiva: los consumidores no pueden modificar la lista interna. */
    public ParsedDeckList {
        entries = List.copyOf(entries);
    }
}
