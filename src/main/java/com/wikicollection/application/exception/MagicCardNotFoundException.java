package com.wikicollection.application.exception;

public class MagicCardNotFoundException extends RuntimeException {

    public MagicCardNotFoundException(String message) {
        super(message);
    }
}
