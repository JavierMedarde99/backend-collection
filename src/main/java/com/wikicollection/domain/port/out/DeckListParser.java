package com.wikicollection.domain.port.out;

import com.wikicollection.domain.model.DeckImportFormat;
import com.wikicollection.domain.model.ParsedDeckList;

/**
 * Puerto de salida: convierte el contenido de un archivo de lista de mazo en entradas de dominio.
 * Cada formato (TXT, JSON, CSV) tiene su adaptador.
 */
public interface DeckListParser {

    /**
     * Formato de archivo que consume este parser.
     */
    DeckImportFormat format();

    /**
     * Parsea el contenido completo del archivo.
     *
     * @param content contenido del archivo
     * @return las entradas encontradas
     * @throws com.wikicollection.application.exception.DeckListParseException si el contenido
     *         está vacío, no contiene ninguna carta o alguna línea no es válida
     */
    ParsedDeckList parse(String content);
}
