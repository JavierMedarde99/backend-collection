package com.wikicollection.application.exception;

public class DeckNotFoundException extends RuntimeException {

    public DeckNotFoundException(String message) {
        super(message);
    }
}
