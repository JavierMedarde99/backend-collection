package com.wikicollection.domain.model;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Una adición pedida de alta: su externalId de RAWG y el estado en el que nace,
 * con los datos de adquisición y seguimiento que el usuario haya rellenado.
 *
 * <p>{@code link} marca las que ya están en la colección del usuario y solo se
 * quieren enlazar al base: no exigen datos y conservan los suyos.
 *
 * <p>La forma legada ({@code externalIds} + {@code status}) se traduce a semillas
 * sin datos: en PLAYING/COMPLETED el servicio pone la fecha de inicio de hoy
 * para conservar el comportamiento anterior; en OWNED/WISHLIST no inventa
 * fechas ni adquisición.
 */
public record GameAdditionSeed(
        String externalId,
        GameStatus status,
        LocalDate acquisitionDate,
        BigDecimal acquisitionPrice,
        LocalDate dateAdded,
        LocalDate dateCompleted,
        boolean link) {
}
