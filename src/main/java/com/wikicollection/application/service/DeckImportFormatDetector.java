package com.wikicollection.application.service;

import java.util.Locale;

import com.wikicollection.domain.model.DeckImportFormat;

import org.springframework.stereotype.Component;

/**
 * Deduce de qué formato es un archivo de lista de mazo (#353).
 *
 * <p>La extensión gana cuando la hay, porque el cliente sabe de qué salió el archivo aunque
 * su contenido sea ambiguo. Si no la hay, se deduce del contenido y, si tampoco se puede, se
 * asume texto plano: es el formato que exportan Arena, Moxfield y Deckbox, y el parser de
 * texto plano ignora las líneas que no son cartas en vez de fallar, así que un archivo mal
 * etiquetado no rompe nada.
 *
 * <p>{@link #requireConsistent} es la parte que evita el error silencioso: si el cliente
 * declara un formato y el contenido no encaja, se rechaza con 400 en lugar de dejar que el
 * parser falle en segundo plano, donde el usuario solo vería un job en FAILED sin explicación
 * de que el problema era su declaración.
 */
@Component
public class DeckImportFormatDetector {

    /**
     * @param content contenido del archivo
     * @param filename nombre del archivo, o {@code null} si no se envió
     * @return el formato deducido, o el de la extensión; {@code TXT} si no se puede saber
     */
    public DeckImportFormat detect(String content, String filename) {
        DeckImportFormat fromFilename = fromFilename(filename);
        if (fromFilename != null) {
            return fromFilename;
        }
        DeckImportFormat fromContent = fromContent(content);
        return fromContent == null ? DeckImportFormat.TXT : fromContent;
    }

    /**
     * Rechaza un formato declarado que no encaja con el contenido.
     *
     * @throws IllegalArgumentException si el contenido demuestra que el formato declarado no
     *         es el del archivo
     */
    public void requireConsistent(DeckImportFormat declared, String content, String filename) {
        if (declared == null) {
            return;
        }
        DeckImportFormat extension = fromFilename(filename);
        if (extension != null) {
            if (extension != declared) {
                throw new IllegalArgumentException("El archivo \"" + filename + "\" se declara como " + declared
                        + " pero su extensión indica " + extension);
            }
            return;
        }
        // Aquí, a diferencia de detect(), "sin marcas" significa TXT y no "no se sabe": hay un
        // formato declarado contra el que comparar, y un archivo sin marcas de JSON ni de CSV
        // es un archivo de texto plano. Dejarlo pasar convertiría un 400 explicativo en un job
        // en FAILED cuyo único síntoma es que el parser no entendió nada.
        DeckImportFormat deduced = fromContent(content);
        if (deduced == null) {
            deduced = DeckImportFormat.TXT;
        }
        if (deduced != declared) {
            throw new IllegalArgumentException("El archivo se declara como " + declared
                    + " pero su contenido parece " + deduced);
        }
    }

    private DeckImportFormat fromFilename(String filename) {
        if (filename == null || filename.isBlank()) {
            return null;
        }
        int dot = filename.lastIndexOf('.');
        if (dot < 0 || dot == filename.length() - 1) {
            return null;
        }
        return switch (filename.substring(dot + 1).toLowerCase(Locale.ROOT)) {
            case "txt" -> DeckImportFormat.TXT;
            case "json" -> DeckImportFormat.JSON;
            case "csv" -> DeckImportFormat.CSV;
            default -> null;
        };
    }

    /**
     * Deduce del contenido. Devuelve {@code null} cuando no hay señal clara, que es distinto
     * de "es texto plano": texto plano no tiene ninguna marca.
     */
    private DeckImportFormat fromContent(String content) {
        if (content == null || content.isBlank()) {
            return null;
        }
        String trimmed = content.stripLeading();
        if (trimmed.startsWith("{") || trimmed.startsWith("[")) {
            return DeckImportFormat.JSON;
        }
        String firstLine = content.lines().filter(line -> !line.isBlank()).findFirst().orElse("");
        String lower = firstLine.toLowerCase(Locale.ROOT);
        if ((lower.contains(",") || lower.contains(";"))
                && (lower.contains("name") || lower.contains("nombre"))
                && !lower.matches("^\\s*\\d+.*")) {
            return DeckImportFormat.CSV;
        }
        return null;
    }
}