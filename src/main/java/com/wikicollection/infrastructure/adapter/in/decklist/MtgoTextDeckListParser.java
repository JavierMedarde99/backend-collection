package com.wikicollection.infrastructure.adapter.in.decklist;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.wikicollection.application.exception.DeckListParseException;
import com.wikicollection.domain.model.DeckImportFormat;
import com.wikicollection.domain.model.DeckListEntry;
import com.wikicollection.domain.model.ParsedDeckList;
import com.wikicollection.domain.port.out.DeckListParser;

import org.springframework.stereotype.Component;

/**
 * Parser de listas de mazo en texto plano (estilo MTGO/Arena): zonas COMMANDER, DECK y
 * SIDEBOARD, una carta por línea con el formato {@code cantidad nombre (SET) numero}.
 */
@Component
public class MtgoTextDeckListParser implements DeckListParser {

    /**
     * Línea de carta: cantidad, nombre y, opcionalmente, set entre paréntesis, número de
     * coleccionista, indicador de foil y precio. Lo que no encaja no se considera carta.
     */
    public static final Pattern CARD_LINE = Pattern.compile(
            "^(?<qty>\\d+)\\s+(?<name>.+?)(?:\\s+\\((?<set>[A-Z0-9]{2,6})\\))?"
                    + "(?:\\s+(?<num>\\d+))?(?:\\s+\\*(?:F|NF)\\*)?(?:\\s+\\$[\\d.,]+)?$");

    /** Zona del archivo en la que se está leyendo la línea actual. */
    private enum Zona {
        COMMANDER, DECK, SIDEBOARD
    }

    @Override
    public DeckImportFormat format() {
        return DeckImportFormat.TXT;
    }

    @Override
    public ParsedDeckList parse(String content) {
        if (content == null || content.isBlank()) {
            throw new DeckListParseException("La lista de mazo está vacía");
        }

        List<DeckListEntry> entradas = new ArrayList<>();
        int sideboardIgnored = 0;
        Zona zona = Zona.DECK;

        String[] lineas = content.split("\\R", -1);
        for (int i = 0; i < lineas.length; i++) {
            String linea = lineas[i].trim();
            int numero = i + 1;

            // Línea vacía o comentario: se ignora
            if (linea.isEmpty() || linea.startsWith("//")) {
                continue;
            }

            // Cabecera de zona en línea propia: cambia la zona actual
            if (linea.equalsIgnoreCase("COMMANDER")) {
                zona = Zona.COMMANDER;
                continue;
            }
            if (linea.equalsIgnoreCase("DECK")) {
                zona = Zona.DECK;
                continue;
            }
            if (linea.equalsIgnoreCase("SIDEBOARD")) {
                zona = Zona.SIDEBOARD;
                continue;
            }

            // El sideboard no se importa
            if (zona == Zona.SIDEBOARD) {
                sideboardIgnored++;
                continue;
            }

            // Una línea que no es carta no es un error del archivo
            Matcher matcher = CARD_LINE.matcher(linea);
            if (!matcher.matches()) {
                continue;
            }

            int cantidad = Integer.parseInt(matcher.group("qty"));
            if (cantidad < 1) {
                throw new DeckListParseException(
                        "Cantidad no válida en la línea " + numero + ": " + linea);
            }

            String nombre = matcher.group("name").replaceAll("\\s+", " ").trim();
            entradas.add(new DeckListEntry(numero, cantidad, nombre, matcher.group("set"),
                    matcher.group("num"), zona == Zona.COMMANDER));
        }

        if (entradas.isEmpty()) {
            throw new DeckListParseException("La lista de mazo no contiene ninguna carta");
        }

        return new ParsedDeckList(entradas, sideboardIgnored);
    }
}
