package com.wikicollection.application.exception;

public class BookConflictException extends RuntimeException {

    public BookConflictException(String message) {
        super(message);
    }
}