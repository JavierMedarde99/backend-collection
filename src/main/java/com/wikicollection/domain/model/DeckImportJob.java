package com.wikicollection.domain.model;

import java.time.Instant;
import java.util.List;

/**
 * Un trabajo de importación de mazos (#353).
 *
 * <p>Es un record inmutable que el worker reescribe entero con cada avance, de modo que el
 * {@code GET} de estado nunca lee un job a medias. Los métodos de copia arrastran a mano los
 * campos que no cambian en esa transición (dueño, mazo, formato, modo, creación): por eso
 * existen y por eso hay un test que los fija.
 *
 * <p>Su ciclo de vida vive en memoria (Caffeine), no en Mongo: un reinicio borra los jobs y un
 * {@code GET} de uno caducado responde 404. Reenviar el archivo es seguro en modo REPLACE.
 */
public record DeckImportJob(
        String jobId,
        String deckId,
        String ownerId,
        DeckImportFormat format,
        DeckImportMode mode,
        DeckImportStatus status,
        DeckImportPhase phase,
        DeckImportProgress progress,
        String commanderName,
        List<String> commanderColors,
        List<UnresolvedCardEntry> unresolved,
        DeckStatusReport validation,
        String error,
        Instant createdAt,
        Instant updatedAt,
        Instant completedAt) {

    /** Job recién aceptado, todavía en cola. */
    public static DeckImportJob pending(String jobId, String deckId, String ownerId,
                                        DeckImportFormat format, DeckImportMode mode) {
        Instant now = Instant.now();
        return new DeckImportJob(jobId, deckId, ownerId, format, mode,
                DeckImportStatus.PENDING, DeckImportPhase.PARSING,
                new DeckImportProgress(0, 0, 0, 0),
                null, List.of(), List.of(), null, null,
                now, now, null);
    }

    /** El job sigue vivo: nuevo estado, nuevo progreso, mismas cartas pendientes. */
    public DeckImportJob progress(DeckImportStatus status, DeckImportPhase phase,
                                  DeckImportProgress progress, List<UnresolvedCardEntry> unresolved) {
        return new DeckImportJob(jobId, deckId, ownerId, format, mode, status, phase,
                progress, commanderName, commanderColors, copyOf(unresolved), validation, error,
                createdAt, Instant.now(), completedAt);
    }

    /**
     * El comandante se resuelve antes que el resto del mazo: se anota en cuanto se sabe, y de
     * él dependen los colores con los que {@code DeckValidator} juzga la identidad de color.
     * La lista de colores es la que trae la respuesta de Scryfall, compartida con la caché de
     * búsquedas: se copia por el mismo motivo que {@link #progress}.
     */
    public DeckImportJob commander(String commanderName, List<String> commanderColors) {
        return new DeckImportJob(jobId, deckId, ownerId, format, mode, status, phase,
                progress, commanderName, copyOf(commanderColors), unresolved, validation, error,
                createdAt, Instant.now(), completedAt);
    }

    /** El mazo está guardado y validado. Que alguna carta quede sin resolver no lo impide. */
    public DeckImportJob completed(DeckStatusReport validation, String commanderName,
                                   List<String> commanderColors) {
        Instant now = Instant.now();
        return new DeckImportJob(jobId, deckId, ownerId, format, mode,
                DeckImportStatus.COMPLETED, DeckImportPhase.DONE,
                progress, commanderName, copyOf(commanderColors), unresolved, validation, null,
                createdAt, now, now);
    }

    /** El job murió. El mazo no se ha tocado. */
    public DeckImportJob failed(String error) {
        Instant now = Instant.now();
        return new DeckImportJob(jobId, deckId, ownerId, format, mode,
                DeckImportStatus.FAILED, DeckImportPhase.DONE,
                progress, commanderName, commanderColors, unresolved, validation, error,
                createdAt, now, now);
    }

    /**
     * Fotografía de una lista que viene de fuera del job. El worker sigue añadiendo entradas
     * a la suya después de publicar el job en la caché, así que guardar la referencia haría
     * que el {@code GET} de estado leyera una lista que está cambiando debajo;
     * {@code List.copyOf} deja además esa lista de solo lectura.
     */
    private static <T> List<T> copyOf(List<T> list) {
        return list == null ? null : List.copyOf(list);
    }
}