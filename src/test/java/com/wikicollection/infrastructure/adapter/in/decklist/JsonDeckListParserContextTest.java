package com.wikicollection.infrastructure.adapter.in.decklist;

import static org.assertj.core.api.Assertions.assertThat;

import com.wikicollection.domain.model.DeckImportFormat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.util.ReflectionTestUtils;

import tools.jackson.databind.ObjectMapper;

/**
 * El único constructor del parser pide el {@code ObjectMapper} que la app sí tiene
 * (Jackson 3, autoconfigurado por Boot 4). Si ese bean desapareciera o el constructor
 * cambiara a un tipo que Spring no pueda resolver, este contexto no arrancaría.
 */
@SpringBootTest(properties = {"spring.data.mongodb.auto-index-creation=false",
        "app.boardgame-status-migration.enabled=false" })
class JsonDeckListParserContextTest {

    @Autowired
    private JsonDeckListParser parser;

    /** El bean de ObjectMapper de la aplicación: el mismo que debería usar el parser. */
    @Autowired
    private ObjectMapper mapper;

    @Test
    void springResolvesTheParserWithTheAppMapperBean() {
        // No basta con que el contexto arranque: el mapper del parser tiene que ser el
        // MISMO bean que usa la app, no uno creado por el propio parser
        assertThat(ReflectionTestUtils.getField(parser, "mapper")).isSameAs(mapper);
        assertThat(parser.format()).isEqualTo(DeckImportFormat.JSON);
        assertThat(parser.parse("[{\"name\":\"Sol Ring\"}]").entries()).hasSize(1);
    }
}
