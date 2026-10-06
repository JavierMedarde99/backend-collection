package com.wikicollection.domain.model;

/**
 * Una línea con carta de un archivo de lista de mazo, tal y como aparece en el archivo.
 *
 * @param line            número de línea (1-based) de la que salió la entrada
 * @param quantity        cantidad indicada en la línea
 * @param name            nombre de la carta, con los espacios normalizados
 * @param setCode         código de set entre paréntesis, o {@code null} si la línea no lo traía
 * @param collectorNumber número de coleccionista, o {@code null} si la línea no lo traía
 * @param commander       {@code true} si la línea estaba en la zona COMMANDER
 */
public record DeckListEntry(int line, int quantity, String name,
                            String setCode, String collectorNumber, boolean commander) {
}
