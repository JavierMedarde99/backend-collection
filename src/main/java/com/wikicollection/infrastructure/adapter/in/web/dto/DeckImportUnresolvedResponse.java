package com.wikicollection.infrastructure.adapter.in.web.dto;

import java.util.List;

/**
 * Una línea del archivo que no acabó en el mazo (#353).
 *
 * @param line número de línea, para que el usuario la encuentre en su archivo
 * @param raw la línea tal cual se leyó
 * @param quantity cuántas copias pedía
 * @param name el nombre que se buscó
 * @param reason {@code NOT_FOUND}, {@code AMBIGUOUS} o {@code UPSTREAM_ERROR}
 * @param candidates cartas que Scryfall propuso, para que el usuario elija
 */
public record DeckImportUnresolvedResponse(
        int line,
        String raw,
        int quantity,
        String name,
        String reason,
        List<DeckImportCandidateResponse> candidates) {
}