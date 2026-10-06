package com.wikicollection.infrastructure.adapter.in.web.dto;

/**
 * Avance de una importación (#353).
 *
 * @param total líneas de carta del archivo, sin el sideboard
 * @param processed líneas ya resueltas o marcadas como irresolubles
 * @param resolved de esas, las que encontraron su carta (el comandante cuenta una)
 * @param sideboardIgnored líneas del sideboard, que el importador ignora
 */
public record DeckImportProgressResponse(
        int total,
        int processed,
        int resolved,
        int sideboardIgnored) {
}