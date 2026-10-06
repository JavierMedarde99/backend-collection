package com.wikicollection.infrastructure.adapter.in.web.dto;

import java.time.Instant;
import java.util.List;

/**
 * Estado de una importación de mazo (#353).
 *
 * @param status {@code PENDING}, {@code RUNNING}, {@code COMPLETED} o {@code FAILED}
 * @param phase en qué punto va: {@code PARSING}, {@code RESOLVING}, {@code SAVING} o {@code DONE}
 * @param deck el mazo ya guardado, o {@code null} mientras el job no haya terminado. Mongo es
 *             la fuente de verdad del mazo, así que no se devuelve nada hasta que existe.
 * @param commander nombre del comandante resuelto, si lo hubo
 * @param commanderColors identidad de color del comandante, de la que dependen las reglas
 * @param unresolved líneas del archivo que no entraron en el mazo, con sus candidatos
 * @param validation resultado de validar el mazo, o {@code null} si el job no terminó bien
 * @param error por qué falló, o {@code null} si no falló
 */
public record DeckImportJobResponse(
        String jobId,
        String status,
        String phase,
        DeckResponse deck,
        String commander,
        List<String> commanderColors,
        List<DeckImportUnresolvedResponse> unresolved,
        DeckImportValidationResponse validation,
        String error,
        DeckImportProgressResponse progress,
        Instant createdAt,
        Instant updatedAt,
        Instant completedAt) {
}