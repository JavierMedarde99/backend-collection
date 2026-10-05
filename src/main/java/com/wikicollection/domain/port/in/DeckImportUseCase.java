package com.wikicollection.domain.port.in;

import com.wikicollection.domain.model.DeckImportFormat;
import com.wikicollection.domain.model.DeckImportJob;
import com.wikicollection.domain.model.DeckImportMode;

/**
 * Importar una lista de mazo a un mazo existente (#353).
 */
public interface DeckImportUseCase {

    /**
     * Acepta la importación y la encola.
     *
     * @param deckId mazo destino
     * @param content contenido del archivo
     * @param format formato declarado, o {@code null} para deducirlo del contenido
     * @param mode qué hacer con las cartas que el mazo ya tenía
     * @param userId dueño que hace la importación
     * @return el job recién creado, en {@code PENDING}
     */
    DeckImportJob startImport(String deckId, String content, DeckImportFormat format,
                              DeckImportMode mode, String userId);

    /**
     * Estado de una importación. El mazo es la fuente de verdad: este método devuelve el job,
     * no el mazo.
     *
     * @throws com.wikicollection.application.exception.DeckImportNotFoundException si el job
     *         no existe o no es de este mazo
     */
    DeckImportJob findJob(String deckId, String jobId, String userId);
}