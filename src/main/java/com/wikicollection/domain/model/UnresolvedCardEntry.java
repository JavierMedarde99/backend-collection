package com.wikicollection.domain.model;

import java.util.List;

/**
 * Una línea del archivo que no acabó en el mazo (#353).
 *
 * @param line número de línea en el archivo, para que el usuario la encuentre
 * @param raw la línea tal cual venía
 * @param quantity cuántas copias pedía
 * @param name el nombre que se buscó
 * @param reason por qué no se resolvió
 * @param candidates cartas que Scryfall propuso, para que el usuario elija
 */
public record UnresolvedCardEntry(
        int line,
        String raw,
        int quantity,
        String name,
        UnresolvedReason reason,
        List<MagicCardSearchResult> candidates) {

    public UnresolvedCardEntry {
        candidates = List.copyOf(candidates);
    }
}