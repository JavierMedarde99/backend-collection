package com.wikicollection.infrastructure.adapter.in.decklist;

import java.util.ArrayList;
import java.util.List;

import com.wikicollection.application.exception.DeckListParseException;
import com.wikicollection.domain.model.DeckImportFormat;
import com.wikicollection.domain.model.DeckListEntry;
import com.wikicollection.domain.model.ParsedDeckList;
import com.wikicollection.domain.port.out.DeckListParser;

import org.springframework.stereotype.Component;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

/**
 * Parser de listas de mazo en JSON. Acepta las tres formas de la spec: array de cartas,
 * objeto con la clave {@code cards} y objeto con la clave {@code deck}; la clave
 * {@code commander} del objeto (string u objeto carta) se añade como entrada de
 * comandante.
 *
 * <p>Se recorre el árbol {@link JsonNode} en lugar de mapear a clases/records: así las
 * claves desconocidas de los exports ({@code foil}, {@code currency}, {@code price}…) se
 * ignoran pase lo que pase con la configuración del mapper.
 */
@Component
public class JsonDeckListParser implements DeckListParser {

    private final ObjectMapper mapper;

    /**
     * Único constructor: Spring inyecta el {@code ObjectMapper} de la aplicación, que en
     * Spring Boot 4 es Jackson 3 ({@code tools.jackson}). Si el bean no existiera, el
     * contexto no arrancaría (lo comprueba {@code JsonDeckListParserContextTest}).
     */
    public JsonDeckListParser(ObjectMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public DeckImportFormat format() {
        return DeckImportFormat.JSON;
    }

    @Override
    public ParsedDeckList parse(String content) {
        if (content == null || content.isBlank()) {
            throw new DeckListParseException("La lista de mazo está vacía");
        }

        JsonNode raiz;
        try {
            // Jackson 3 comprueba que no quede ningún token tras el árbol ("Trailing
            // token … found after value", siempre activo en readTree), así que la basura
            // tras el primer valor es error de parseo; Jackson 2 la aceptaba. La excepción
            // es JacksonException, unchecked en Jackson 3 (no un catch de RuntimeException
            // amplio, que se tragaría los DeckListParseException de este parser).
            raiz = mapper.readTree(content);
        } catch (JacksonException e) {
            throw new DeckListParseException("El JSON de la lista de mazo no es válido");
        }

        List<DeckListEntry> entradas = new ArrayList<>();
        if (raiz != null && raiz.isArray()) {
            for (JsonNode carta : raiz) {
                // Un elemento que no es objeto no encaja con el formato: se ignora
                if (carta == null || !carta.isObject()) {
                    continue;
                }
                entradas.add(entradaDe(carta, entradas.size() + 1));
            }
        } else if (raiz != null && raiz.isObject()) {
            JsonNode cartas = nodoCartas(raiz);
            if (cartas != null) {
                for (JsonNode carta : cartas) {
                    if (carta == null || !carta.isObject()) {
                        continue;
                    }
                    entradas.add(entradaDe(carta, entradas.size() + 1));
                }
            }
            DeckListEntry comandante = comandante(raiz.get("commander"), entradas.size() + 1);
            if (comandante != null) {
                entradas.add(comandante);
            }
        }

        if (entradas.isEmpty()) {
            throw new DeckListParseException("La lista de mazo no contiene ninguna carta");
        }

        return new ParsedDeckList(entradas, 0);
    }

    /** Array de cartas de las formas de la spec: la primera clave presente que lo sea. */
    private JsonNode nodoCartas(JsonNode raiz) {
        for (String clave : new String[] { "cards", "deck" }) {
            JsonNode cartas = raiz.get(clave);
            if (cartas != null && cartas.isArray()) {
                return cartas;
            }
        }
        return null;
    }

    /**
     * La clave {@code commander} del objeto raíz: string con el nombre, u objeto carta
     * con sus metadatos. Lo que no encaja (vacío, otro tipo) se ignora.
     */
    private DeckListEntry comandante(JsonNode nodo, int posicion) {
        if (nodo == null || nodo.isNull()) {
            return null;
        }
        if (nodo.isTextual() && !nodo.asText().isBlank()) {
            return new DeckListEntry(posicion, 1, nodo.asText().trim(), null, null, true);
        }
        if (nodo.isObject()) {
            return entradaDe(nodo, posicion, true);
        }
        return null;
    }

    private DeckListEntry entradaDe(JsonNode carta, int posicion) {
        return entradaDe(carta, posicion, false);
    }

    private DeckListEntry entradaDe(JsonNode carta, int posicion, boolean comandante) {
        // Todos los llamantes garantizan que es objeto (lo demás se salta en los bucles)
        JsonNode nombre = carta.get("name");
        if (nombre == null || !nombre.isTextual() || nombre.asText().isBlank()) {
            throw new DeckListParseException(
                    "Entrada de carta sin nombre en la posición " + posicion);
        }
        String nombreCarta = nombre.asText().trim();
        JsonNode marcador = carta.get("isCommander");
        boolean esComandante = comandante || (marcador != null && marcador.asBoolean(false));
        return new DeckListEntry(posicion, leerCantidad(carta, nombreCarta, posicion),
                nombreCarta, texto(carta, "set", "setCode"),
                texto(carta, "number", "collectorNumber"), esComandante);
    }

    /** Cantidad: ausente o en blanco → 1; si existe tiene que ser un entero ≥ 1. */
    private int leerCantidad(JsonNode carta, String nombreCarta, int posicion) {
        JsonNode cantidad = carta.get("quantity");
        if (cantidad == null || cantidad.isNull()
                || (cantidad.isTextual() && cantidad.asText().isBlank())) {
            return 1;
        }
        int valor;
        if (cantidad.isIntegralNumber()) {
            // intValue() truncaría en silencio fuera de rango (5000000000 → 705032704)
            if (!cantidad.canConvertToInt()) {
                throw cantidadInvalida(posicion, nombreCarta);
            }
            valor = cantidad.intValue();
        } else {
            try {
                // Cubre textos y números no enteros: asText() no lanza, parseInt sí
                valor = Integer.parseInt(cantidad.asText().trim());
            } catch (NumberFormatException e) {
                throw cantidadInvalida(posicion, nombreCarta);
            }
        }
        if (valor < 1) {
            throw cantidadInvalida(posicion, nombreCarta);
        }
        return valor;
    }

    /** Primera clave de las dos variantes (alias incluidos) con valor de texto o número. */
    private String texto(JsonNode carta, String... claves) {
        for (String clave : claves) {
            JsonNode nodo = carta.get(clave);
            if (nodo != null && nodo.isTextual() && !nodo.asText().isBlank()) {
                return nodo.asText().trim();
            }
            if (nodo != null && nodo.isIntegralNumber()) {
                // asText() conserva todos los dígitos; intValue() truncaría fuera de rango
                return nodo.asText();
            }
        }
        return null;
    }

    private DeckListParseException cantidadInvalida(int posicion, String nombreCarta) {
        return new DeckListParseException(
                "Cantidad no válida en la entrada " + posicion + " (" + nombreCarta + ")");
    }
}
