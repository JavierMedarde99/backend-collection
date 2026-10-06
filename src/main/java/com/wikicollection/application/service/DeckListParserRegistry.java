package com.wikicollection.application.service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.wikicollection.domain.model.DeckImportFormat;
import com.wikicollection.domain.port.out.DeckListParser;

import org.springframework.stereotype.Component;

/**
 * Índice de los parsers de lista de mazo por formato. Spring inyecta la lista de
 * {@link DeckListParser} en orden indeterminado, así que el mapa se construye a partir
 * de {@code parser.format()}: pedir el parser de un formato no depende de la posición
 * en la que llegue cada bean.
 *
 * <p>Vive en {@code application/} y no junto a los parsers, que están en el adaptador de
 * entrada, porque lo consume el worker de importación (#353). Si estuviera allí, el
 * servicio tendría que importar hacia fuera de la capa.
 */
@Component
public class DeckListParserRegistry {

    private final Map<DeckImportFormat, DeckListParser> parsers;

    public DeckListParserRegistry(List<DeckListParser> parsers) {
        this.parsers = new HashMap<>();
        for (DeckListParser parser : parsers) {
            this.parsers.put(parser.format(), parser);
        }
    }

    /**
     * Parser de un formato concreto.
     *
     * @param format formato del archivo
     * @return el parser que lo consume
     * @throws IllegalArgumentException si ningún parser de la lista atiende ese formato
     */
    public DeckListParser forFormat(DeckImportFormat format) {
        DeckListParser parser = parsers.get(format);
        if (parser == null) {
            throw new IllegalArgumentException("No hay parser para el formato " + format);
        }
        return parser;
    }
}
