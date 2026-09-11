package com.wikicollection.application.exception;

public class MovieShowNotFoundException extends RuntimeException {

    public MovieShowNotFoundException(String message) {
        super(message);
    }
}
