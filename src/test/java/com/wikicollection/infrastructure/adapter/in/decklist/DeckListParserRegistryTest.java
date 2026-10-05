package com.wikicollection.infrastructure.adapter.in.decklist;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;

import com.wikicollection.domain.model.DeckImportFormat;
import com.wikicollection.domain.port.out.DeckListParser;

import org.junit.jupiter.api.Test;

import tools.jackson.databind.json.JsonMapper;

class DeckListParserRegistryTest {

    private final MtgoTextDeckListParser txt = new MtgoTextDeckListParser();
    private final JsonDeckListParser json = new JsonDeckListParser(new JsonMapper());
    private final CsvDeckListParser csv = new CsvDeckListParser();

    @Test
    void returnsTheCorrectParserForEachFormat() {
        // Spring inyecta la lista en orden indeterminado: da igual el orden en que lleguen
        DeckListParserRegistry registry = new DeckListParserRegistry(List.of(csv, json, txt));

        assertThat(registry.forFormat(DeckImportFormat.TXT)).isSameAs(txt);
        assertThat(registry.forFormat(DeckImportFormat.JSON)).isSameAs(json);
        assertThat(registry.forFormat(DeckImportFormat.CSV)).isSameAs(csv);
    }

    @Test
    void unknownFormat_throwsIllegalArgumentException() {
        // Un registro completo cubre los tres valores del enum: sin parser para un
        // formato (aquí, CSV) se comporta como formato desconocido
        DeckListParserRegistry registry = new DeckListParserRegistry(List.of(txt, json));

        assertThatThrownBy(() -> registry.forFormat(DeckImportFormat.CSV))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
