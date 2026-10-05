package com.wikicollection.infrastructure.adapter.in.decklist;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.wikicollection.application.exception.DeckListParseException;
import com.wikicollection.domain.model.DeckImportFormat;
import com.wikicollection.domain.model.DeckListEntry;
import com.wikicollection.domain.model.ParsedDeckList;

import org.junit.jupiter.api.Test;

class JsonDeckListParserTest {

    /**
     * ObjectMapper crudo, construido a pelo: conserva la configuración por defecto de
     * Jackson ({@code FAIL_ON_UNKNOWN_PROPERTIES} activo). Si el parser mapeara a
     * clases/records, los tests fallarían aquí mismo: el parseo debe recorrer el árbol
     * JsonNode y no depender de la configuración del mapper que se le pase.
     */
    private final JsonDeckListParser parser = new JsonDeckListParser(new ObjectMapper());

    @Test
    void parsesArrayOfObjects() {
        ParsedDeckList resultado = parser.parse(
                "[{\"name\":\"Sol Ring\",\"quantity\":4},{\"name\":\"Plains\",\"quantity\":20}]");

        assertThat(resultado.entries()).hasSize(2);
        DeckListEntry solRing = resultado.entries().get(0);
        assertThat(solRing.name()).isEqualTo("Sol Ring");
        assertThat(solRing.quantity()).isEqualTo(4);
        DeckListEntry plains = resultado.entries().get(1);
        assertThat(plains.name()).isEqualTo("Plains");
        assertThat(plains.quantity()).isEqualTo(20);
        assertThat(resultado.sideboardIgnored()).isZero();
    }

    @Test
    void parsesObjectWithCardsAndCommander() {
        ParsedDeckList resultado = parser.parse(
                "{\"cards\":[{\"name\":\"Sol Ring\",\"quantity\":4}],"
                        + "\"commander\":\"Atraxa, Grand Unifier\"}");

        assertThat(resultado.entries()).hasSize(2);
        assertThat(resultado.entries().get(0).commander()).isFalse();
        DeckListEntry comandante = resultado.entries().get(1);
        assertThat(comandante.commander()).isTrue();
        assertThat(comandante.name()).isEqualTo("Atraxa, Grand Unifier");
    }

    @Test
    void parsesObjectWithDeckKeyAndMetadata() {
        ParsedDeckList resultado = parser.parse(
                "{\"deck\":[{\"name\":\"Sol Ring\",\"quantity\":4,\"set\":\"NEO\","
                        + "\"number\":\"269\"}]}");

        assertThat(resultado.entries()).hasSize(1);
        DeckListEntry entrada = resultado.entries().get(0);
        assertThat(entrada.name()).isEqualTo("Sol Ring");
        assertThat(entrada.setCode()).isEqualTo("NEO");
        assertThat(entrada.collectorNumber()).isEqualTo("269");
    }

    @Test
    void honoursIsCommanderFlag() {
        ParsedDeckList resultado = parser.parse(
                "[{\"name\":\"Atraxa, Grand Unifier\",\"isCommander\":true}]");

        assertThat(resultado.entries()).hasSize(1);
        assertThat(resultado.entries().get(0).commander()).isTrue();
    }

    @Test
    void defaultsMissingQuantityToOne() {
        ParsedDeckList resultado = parser.parse("[{\"name\":\"Sol Ring\"}]");

        assertThat(resultado.entries()).hasSize(1);
        assertThat(resultado.entries().get(0).quantity()).isEqualTo(1);
    }

    @Test
    void ignoresUnknownKeys() {
        // Dos entradas: la primera trae claves de export que no existen en el dominio
        ParsedDeckList resultado = parser.parse(
                "[{\"name\":\"Sol Ring\",\"quantity\":4,\"foil\":true,\"currency\":\"USD\"},"
                        + "{\"name\":\"Plains\",\"quantity\":20}]");

        assertThat(resultado.entries()).hasSize(2);
        assertThat(resultado.entries().get(0).name()).isEqualTo("Sol Ring");
        assertThat(resultado.entries().get(0).quantity()).isEqualTo(4);
        assertThat(resultado.entries().get(1).name()).isEqualTo("Plains");
        assertThat(resultado.entries().get(1).quantity()).isEqualTo(20);
    }

    @Test
    void parsesCommanderAsObject() {
        // La spec (6.2) admite que `commander` sea un objeto carta, no solo un string
        ParsedDeckList resultado = parser.parse(
                "{\"cards\":[{\"name\":\"Sol Ring\",\"quantity\":4}],"
                        + "\"commander\":{\"name\":\"Atraxa, Grand Unifier\",\"set\":\"ONE\","
                        + "\"number\":32}}");

        assertThat(resultado.entries()).hasSize(2);
        DeckListEntry comandante = resultado.entries().get(1);
        assertThat(comandante.commander()).isTrue();
        assertThat(comandante.name()).isEqualTo("Atraxa, Grand Unifier");
        assertThat(comandante.setCode()).isEqualTo("ONE");
        // `number` numérico, no entrecomillado: se acepta igual
        assertThat(comandante.collectorNumber()).isEqualTo("32");
    }

    @Test
    void ignoresBlankCommander() {
        ParsedDeckList resultado = parser.parse(
                "{\"cards\":[{\"name\":\"Sol Ring\"}],\"commander\":\"  \"}");

        assertThat(resultado.entries()).hasSize(1);
        assertThat(resultado.entries().get(0).name()).isEqualTo("Sol Ring");
        assertThat(resultado.entries().get(0).commander()).isFalse();
    }

    @Test
    void throwsWhenQuantityIsNotPositive() {
        assertThatThrownBy(() -> parser.parse("[{\"name\":\"Sol Ring\",\"quantity\":0}]"))
                .isInstanceOf(DeckListParseException.class);
    }

    @Test
    void throwsWhenQuantityIsNotANumber() {
        assertThatThrownBy(() -> parser.parse("[{\"name\":\"Sol Ring\",\"quantity\":\"cuatro\"}]"))
                .isInstanceOf(DeckListParseException.class);
    }

    @Test
    void malformedJson_throws() {
        assertThatThrownBy(() -> parser.parse("{\"cards\":[{\"name\":\"Sol Ring\"}"))
                .isInstanceOf(DeckListParseException.class);
    }

    @Test
    void emptyArray_throws() {
        assertThatThrownBy(() -> parser.parse("[]"))
                .isInstanceOf(DeckListParseException.class);
    }

    @Test
    void objectWithoutCards_throws() {
        // Objeto válido pero sin ninguna de las claves de cartas: no contiene ninguna carta
        assertThatThrownBy(() -> parser.parse("{\"nombre\":\"mazo de prueba\"}"))
                .isInstanceOf(DeckListParseException.class);
    }

    @Test
    void entryWithoutName_throws() {
        assertThatThrownBy(() -> parser.parse("[{\"quantity\":4}]"))
                .isInstanceOf(DeckListParseException.class);
    }

    @Test
    void emptyContent_throws() {
        assertThatThrownBy(() -> parser.parse("   \n\n  "))
                .isInstanceOf(DeckListParseException.class);
        assertThatThrownBy(() -> parser.parse(null))
                .isInstanceOf(DeckListParseException.class);
    }

    @Test
    void format_returnsJson() {
        assertThat(parser.format()).isEqualTo(DeckImportFormat.JSON);
    }
}
