package com.wikicollection.application.exception;

public class ImageNotFoundException extends RuntimeException {

    public ImageNotFoundException(String message) {
        super(message);
    }
}
