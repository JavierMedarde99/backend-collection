package com.wikicollection.domain.port.in;

import java.util.List;

import com.wikicollection.domain.model.BoardGameExpansionAddResult;
import com.wikicollection.domain.model.BoardGameExpansionCatalog;

public interface BoardGameExpansionUseCase {

    /**
     * Catálogo de BGG de las expansiones de un juego base, con el flag
     * «ya lo tienes» calculado para el espectador (anónimo ⇒ todo false).
     *
     * <p>404 si el base no existe, 502 si BGG falla.
     */
    BoardGameExpansionCatalog expansionsOf(String baseId, String viewerId);

    /**
     * Da de alta expansiones enlazándolas a su base. Idempotente: lo que ya está
     * en la colección se salta y se informa en el resultado.
     *
     * <p>404 si el base no existe, 403 si es de otro usuario, 422 si la regla de
     * negocio no se cumple, 502 si BGG falla.
     */
    BoardGameExpansionAddResult addExpansions(String baseId, List<String> bggIds, String userId);
}
