package com.wikicollection.application.exception;

/**
 * El alta o la lectura del contenido adicional de un juego no se puede completar
 * por una regla de negocio: el base está en WISHLIST, no viene de RAWG o el
 * externalId pedido no es una adición de ese base (HTTP 422).
 */
public class GameAdditionsException extends RuntimeException {

    public GameAdditionsException(String message) {
        super(message);
    }
}
