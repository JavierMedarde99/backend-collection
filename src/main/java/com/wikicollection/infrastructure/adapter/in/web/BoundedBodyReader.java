package com.wikicollection.infrastructure.adapter.in.web;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

/**
 * Utilidad para leer cuerpos acotados (evita DoS por cuerpos gigantes).
 */
final class BoundedBodyReader {

    private BoundedBodyReader() {
    }

    static String read(InputStream in, long maxBytes, Charset charset) throws IOException {
        if (in == null) {
            throw new IllegalArgumentException("El contenido de la importación está vacío.");
        }
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] buffer = new byte[8192];
        long total = 0;
        int read;
        while ((read = in.read(buffer)) != -1) {
            total += read;
            if (total > maxBytes) {
                throw new IllegalArgumentException("El archivo supera el tamaño máximo de " + maxBytes + " bytes.");
            }
            out.write(buffer, 0, read);
        }
        if (total == 0) {
            throw new IllegalArgumentException("El contenido de la importación está vacío.");
        }
        return out.toString(charset);
    }

    static String readUtf8(InputStream in, long maxBytes) throws IOException {
        return read(in, maxBytes, StandardCharsets.UTF_8);
    }
}
