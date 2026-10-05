package com.wikicollection.infrastructure.adapter.in.decklist;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import com.wikicollection.application.exception.DeckListParseException;
import com.wikicollection.domain.model.DeckImportFormat;
import com.wikicollection.domain.model.DeckListEntry;
import com.wikicollection.domain.model.ParsedDeckList;
import com.wikicollection.domain.port.out.DeckListParser;

import org.springframework.stereotype.Component;

/**
 * Parser de listas de mazo en CSV, con separador {@code ,} o {@code ;} y cabecera
 * obligatoria en la que el orden de las columnas es libre.
 *
 * <p>Sin dependencias externas: se detecta el separador contando los que aparecen fuera
 * de comillas en la cabecera, y los campos se parsean a mano para poder entrecomillar
 * nombres con coma ("Atraxa, Grand Unifier") y escapar comillas dobles ({@code ""}).
 */
@Component
public class CsvDeckListParser implements DeckListParser {

    /** Columnas que reconoce la cabecera, ya en minúsculas. */
    private static final Set<String> COLUMNAS_CONOCIDAS = Set.of(
            "name", "quantity", "set", "setcode", "number", "collectornumber",
            "commander", "iscommander");

    @Override
    public DeckImportFormat format() {
        return DeckImportFormat.CSV;
    }

    @Override
    public ParsedDeckList parse(String content) {
        if (content == null || content.isBlank()) {
            throw new DeckListParseException("La lista de mazo está vacía");
        }

        String[] lineas = content.split("\\R", -1);

        // La primera línea no vacía es la cabecera (el contenido no vacío garantiza que existe)
        int numeroCabecera = 0;
        while (numeroCabecera < lineas.length - 1 && lineas[numeroCabecera].isBlank()) {
            numeroCabecera++;
        }
        String cabeceraCruda = lineas[numeroCabecera];
        if (cabeceraCruda.startsWith("\uFEFF")) {
            cabeceraCruda = cabeceraCruda.substring(1);
        }

        char separador = detectarSeparador(cabeceraCruda);
        List<String> cabecera = new ArrayList<>();
        for (String columna : partirCampos(cabeceraCruda, separador)) {
            cabecera.add(columna.trim().toLowerCase(Locale.ROOT));
        }

        if (cabecera.stream().noneMatch(COLUMNAS_CONOCIDAS::contains)) {
            throw new DeckListParseException("La lista de mazo no tiene cabecera");
        }
        int columnaNombre = indice(cabecera, "name");
        if (columnaNombre < 0) {
            throw new DeckListParseException("La cabecera no tiene la columna name");
        }
        int columnaCantidad = indice(cabecera, "quantity");
        int columnaSet = indice(cabecera, "set", "setcode");
        int columnaNumero = indice(cabecera, "number", "collectornumber");
        int columnaComandante = indice(cabecera, "commander", "iscommander");

        List<DeckListEntry> entradas = new ArrayList<>();
        for (int i = numeroCabecera + 1; i < lineas.length; i++) {
            if (lineas[i].isBlank()) {
                continue;
            }
            List<String> campos = partirCampos(lineas[i], separador);
            String nombre = valor(campos, columnaNombre);
            // Una fila sin nombre no es una carta: se ignora, como en el resto de parsers
            if (nombre == null) {
                continue;
            }
            int numeroLinea = i + 1;
            entradas.add(new DeckListEntry(numeroLinea,
                    leerCantidad(valor(campos, columnaCantidad), nombre, numeroLinea),
                    nombre, valor(campos, columnaSet), valor(campos, columnaNumero),
                    "true".equalsIgnoreCase(valor(campos, columnaComandante))));
        }

        if (entradas.isEmpty()) {
            throw new DeckListParseException("La lista de mazo no contiene ninguna carta");
        }

        return new ParsedDeckList(entradas, 0);
    }

    /**
     * El separador es el que más veces aparece fuera de comillas en la cabecera (empate →
     * coma). Los cuenta el propio {@link #partirCampos}, así que la regla de comillas
     * (incluido el {@code ""} escapado) es exactamente la que luego parsea las filas.
     */
    private char detectarSeparador(String cabecera) {
        int comas = partirCampos(cabecera, ',').size() - 1;
        int puntoycomas = partirCampos(cabecera, ';').size() - 1;
        return puntoycomas > comas ? ';' : ',';
    }

    /**
     * Parte una línea en campos separados por {@code separador}. Lo que está entre
     * comillas va a un solo campo (coma incluida) y {@code ""} es una comilla literal.
     */
    private List<String> partirCampos(String linea, char separador) {
        List<String> campos = new ArrayList<>();
        StringBuilder actual = new StringBuilder();
        boolean entreComillas = false;
        for (int i = 0; i < linea.length(); i++) {
            char c = linea.charAt(i);
            if (entreComillas) {
                if (c != '"') {
                    actual.append(c);
                } else if (i + 1 < linea.length() && linea.charAt(i + 1) == '"') {
                    actual.append('"');
                    i++;
                } else {
                    entreComillas = false;
                }
            } else if (c == '"') {
                entreComillas = true;
            } else if (c == separador) {
                campos.add(actual.toString());
                actual.setLength(0);
            } else {
                actual.append(c);
            }
        }
        campos.add(actual.toString());
        return campos;
    }

    /** Índice de la primera columna con alguno de los nombres dados, o -1. */
    private int indice(List<String> cabecera, String... nombres) {
        for (String nombre : nombres) {
            int indice = cabecera.indexOf(nombre);
            if (indice >= 0) {
                return indice;
            }
        }
        return -1;
    }

    /** Valor de una columna en una fila: recortado, o null si no está o está en blanco. */
    private String valor(List<String> campos, int columna) {
        if (columna < 0 || columna >= campos.size()) {
            return null;
        }
        String valor = campos.get(columna).trim();
        return valor.isEmpty() ? null : valor;
    }

    /** Cantidad: ausente o en blanco → 1; si existe tiene que ser un entero ≥ 1. */
    private int leerCantidad(String cantidad, String nombreCarta, int linea) {
        if (cantidad == null) {
            return 1;
        }
        int valor;
        try {
            valor = Integer.parseInt(cantidad);
        } catch (NumberFormatException e) {
            throw cantidadInvalida(nombreCarta, linea);
        }
        if (valor < 1) {
            throw cantidadInvalida(nombreCarta, linea);
        }
        return valor;
    }

    private DeckListParseException cantidadInvalida(String nombreCarta, int linea) {
        return new DeckListParseException(
                "Cantidad no válida en la línea " + linea + ": " + nombreCarta);
    }
}
