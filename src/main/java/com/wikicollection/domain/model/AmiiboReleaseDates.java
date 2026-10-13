package com.wikicollection.domain.model;

import java.time.LocalDate;

/**
 * Fechas de lanzamiento de un amiibo por región, tal y como las expone
 * AmiiboAPI en el objeto {@code release} ({@code {au, eu, jp, na}}).
 * Cualquier región puede venir ausente (lanzamiento no anunciado o no
 * verificado en el catálogo).
 */
public record AmiiboReleaseDates(
        LocalDate au,
        LocalDate eu,
        LocalDate jp,
        LocalDate na) {
}