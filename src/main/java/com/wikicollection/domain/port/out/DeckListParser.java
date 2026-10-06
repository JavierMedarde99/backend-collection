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
     * <p>Las líneas que no son cartas (comentarios, líneas en blanco, cabeceras de zona o
     * texto que no encaja en el formato) se <strong>ignoran</strong>: una línea rara no es
     * un error del archivo. Solo se lanza excepción por contenido vacío, por un archivo sin
     * ninguna carta y por una cantidad de carta no válida.
     *
     * @param content contenido del archivo
     * @return las entradas encontradas
     * @throws com.wikicollection.application.exception.DeckListParseException si el contenido
     *         está vacío, no contiene ninguna carta o una línea de carta indica una cantidad
     *         no válida (menor que 1 o demasiado grande para un entero)
     */
    ParsedDeckList parse(String content);
}
