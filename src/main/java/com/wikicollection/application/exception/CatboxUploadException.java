package com.wikicollection.application.exception;

public class CatboxUploadException extends RuntimeException {

    public CatboxUploadException(String message) {
        super(message);
    }

    public CatboxUploadException(String message, Throwable cause) {
        super(message, cause);
    }
}
