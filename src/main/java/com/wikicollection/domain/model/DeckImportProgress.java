package com.wikicollection.domain.model;

/**
 * Avance de un trabajo de importación (#353).
 *
 * @param total líneas de carta del archivo, sin contar el sideboard
 * @param processed líneas ya resueltas o marcadas como irresolubles
 * @param resolved de esas, las que sí encontraron su carta
 * @param sideboardIgnored líneas del sideboard, que el importador ignora
 */
public record DeckImportProgress(int total, int processed, int resolved, int sideboardIgnored) {
}