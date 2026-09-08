package com.wikicollection.application.exception;

public class BoardGameNotFoundException extends RuntimeException {

    public BoardGameNotFoundException(String message) {
        super(message);
    }
}
