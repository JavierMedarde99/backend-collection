package com.wikicollection.domain.model;

/**
 * Tipos de amiibo del catálogo de AmiiboAPI, mapeados directamente de los
 * valores que devuelve la API ({@code Card}, {@code Figure}, {@code Band},
 * {@code Yarn}, {@code Block}). Un valor desconocido del catálogo se mapea a
 * {@code null} (tolerante), no a un error.
 */
public enum AmiiboType {
    CARD, FIGURE, BAND, YARN, BLOCK
}