package com.wikicollection.domain.model;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Una expansión pedida de alta: su bggId y el estado en el que nace, con los
 * datos de adquisición si nace en posesión (OWNED: precio y fecha; WISHLIST:
 * sin precio ni fecha, el servicio se encarga de limpiarlos).
 *
 * <p>La forma legada ({@code bggIds} sueltos) se traduce a semillas en OWNED
 * sin precio y con fecha a repartir por el servicio.
 */
public record BoardGameExpansionSeed(
        String bggId,
        BoardGameStatus status,
        BigDecimal acquisitionPrice,
        LocalDate dateAdded) {
}
