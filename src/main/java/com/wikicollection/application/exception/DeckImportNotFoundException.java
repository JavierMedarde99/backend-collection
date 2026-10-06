package com.wikicollection.application.exception;

/**
 * El trabajo de importación no existe, o no pertenece a este mazo.
 *
 * <p>Las dos cosas son un 404 a propósito: la ruta es por mazo, así que un job que pertenece
 * a otro mazo no está en este recurso. Separar los dos casos dejaría al cliente distinguir
 * "este id nunca existió" de "este id es de otro mazo", que es información que no debe
 * llevarse un 404.
 *
 * <p>Ocurre cuando el job caducó (el registro es en memoria y un reinicio lo vacía), cuando
 * nunca existió, o cuando pertenece a otro mazo.
 */
public class DeckImportNotFoundException extends RuntimeException {

    public DeckImportNotFoundException(String message) {
        super(message);
    }
}