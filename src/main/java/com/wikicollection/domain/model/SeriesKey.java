package com.wikicollection.domain.model;

/**
 * Normalización de la clave de agrupación de una serie.
 *
 * <p>La serie es texto libre, así que "Harry Potter", "harry potter" y
 * "Harry potter " tienen que acabar en el mismo grupo. Se normaliza en el
 * punto de escritura (BookService) para que la búsqueda por serie sea un
 * `is()` exacto en Mongo y no dependa de la caja con la que se escribió.
 */
public final class SeriesKey {

    private SeriesKey() {
    }

    /** Clave de agrupación: minúsculas, sin espacios sobrantes. Null si no hay serie. */
    public static String normalize(String series) {
        if (series == null) {
            return null;
        }
        String normalized = series.trim().replaceAll("\\s+", " ").toLowerCase();
        return normalized.isEmpty() ? null : normalized;
    }
}