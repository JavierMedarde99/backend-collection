package com.wikicollection.application.exception;

/**
 * Un catálogo externo (BoardGameGeek) no contestó, devolvió error o mandó una
 * respuesta ilegible.
 *
 * <p>Es deliberadamente distinto de «no hay resultados», que es una lista vacía
 * válida: el botón de «Añadir expansiones» necesita distinguir «BGG no conoce
 * expansiones de este juego» de «BGG está caído», y responder 502 en el segundo
 * caso (el resto de búsquedas traga el error y devuelve 200 con lista vacía).
 */
public class ExternalCatalogException extends RuntimeException {

    public ExternalCatalogException(String message) {
        super(message);
    }

    public ExternalCatalogException(String message, Throwable cause) {
        super(message, cause);
    }
}
