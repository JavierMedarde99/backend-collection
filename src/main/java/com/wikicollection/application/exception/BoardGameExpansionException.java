package com.wikicollection.application.exception;

/**
 * El alto o la lectura de una expansión no se puede completar por una regla de
 * negocio, no por un fallo técnico: el base no está en OWNED, el bggId no es una
 * expansión en BGG o la expansión no pertenece a ese base (HTTP 422).
 */
public class BoardGameExpansionException extends RuntimeException {

    public BoardGameExpansionException(String message) {
        super(message);
    }
}
