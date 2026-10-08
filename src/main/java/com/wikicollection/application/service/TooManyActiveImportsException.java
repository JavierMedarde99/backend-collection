package com.wikicollection.application.service;

/**
 * Se lanza cuando un usuario ya tiene demasiados trabajos de importación activos (#353).
 */
public class TooManyActiveImportsException extends RuntimeException {

    private final int maxActive;

    public TooManyActiveImportsException(int maxActive) {
        super("Too many active import jobs: " + maxActive);
        this.maxActive = maxActive;
    }

    public int maxActive() {
        return maxActive;
    }
}
