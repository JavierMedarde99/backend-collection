package com.wikicollection.infrastructure.adapter.in.decklist;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.wikicollection.application.exception.DeckListParseException;
import com.wikicollection.domain.model.DeckImportFormat;
import com.wikicollection.domain.model.DeckListEntry;
import com.wikicollection.domain.model.ParsedDeckList;

import org.junit.jupiter.api.Test;

class CsvDeckListParserTest {

    private final CsvDeckListParser parser = new CsvDeckListParser();

    @Test
    void parsesCommaSeparatedWithHeaderInAnyOrder() {
        // El nombre con coma va entrecomillado: la coma de dentro no separa campos
        ParsedDeckList resultado = parser.parse("""
                name,quantity,commander
                Sol Ring,4,false
                "Atraxa, Grand Unifier",1,true
                """);

        assertThat(resultado.entries()).hasSize(2);
        DeckListEntry solRing = resultado.entries().get(0);
        assertThat(solRing.name()).isEqualTo("Sol Ring");
        assertThat(solRing.quantity()).isEqualTo(4);
        assertThat(solRing.commander()).isFalse();
        DeckListEntry atraxa = resultado.entries().get(1);
        assertThat(atraxa.name()).isEqualTo("Atraxa, Grand Unifier");
        assertThat(atraxa.quantity()).isEqualTo(1);
        assertThat(atraxa.commander()).isTrue();
        assertThat(resultado.sideboardIgnored()).isZero();
    }

    @Test
    void parsesSetAndNumberColumns() {
        ParsedDeckList resultado = parser.parse("""
                quantity,name,set,number
                4,"Sol Ring",NEO,269
                """);

        assertThat(resultado.entries()).hasSize(1);
        DeckListEntry entrada = resultado.entries().get(0);
        assertThat(entrada.name()).isEqualTo("Sol Ring");
        assertThat(entrada.quantity()).isEqualTo(4);
        assertThat(entrada.setCode()).isEqualTo("NEO");
        assertThat(entrada.collectorNumber()).isEqualTo("269");
        assertThat(entrada.line()).isEqualTo(2);
    }

    @Test
    void parsesSemicolonSeparated() {
        ParsedDeckList resultado = parser.parse("""
                quantity;name;set
                1;Sol Ring;NEO
                """);

        assertThat(resultado.entries()).hasSize(1);
        DeckListEntry entrada = resultado.entries().get(0);
        assertThat(entrada.quantity()).isEqualTo(1);
        assertThat(entrada.name()).isEqualTo("Sol Ring");
        assertThat(entrada.setCode()).isEqualTo("NEO");
        assertThat(entrada.commander()).isFalse();
    }

    @Test
    void parsesQuotedNameContainingComma() {
        ParsedDeckList resultado = parser.parse("""
                quantity,name
                1,"Sword of Fire and Ice"
                """);

        assertThat(resultado.entries()).hasSize(1);
        assertThat(resultado.entries().get(0).name()).isEqualTo("Sword of Fire and Ice");
        assertThat(resultado.entries().get(0).quantity()).isEqualTo(1);
    }

    @Test
    void parsesEscapedDoubleQuotes() {
        // Fila real: 1,"Atraxa, ""Grand Unifier""" (comillas dobles escapadas dentro del campo)
        String contenido = "quantity,name\n1,\"Atraxa, \"\"Grand Unifier\"\"\"\n";

        ParsedDeckList resultado = parser.parse(contenido);

        assertThat(resultado.entries()).hasSize(1);
        assertThat(resultado.entries().get(0).name()).isEqualTo("Atraxa, \"Grand Unifier\"");
    }

    @Test
    void headerAfterUtf8BomIsRecognised() {
        // El BOM que pone Excel en los exports CSV rompería la cabecera si no se quita
        ParsedDeckList resultado = parser.parse("\uFEFFquantity,name\n4,Sol Ring\n");

        assertThat(resultado.entries()).hasSize(1);
        assertThat(resultado.entries().get(0).name()).isEqualTo("Sol Ring");
    }

    @Test
    void headerIsTheFirstNonEmptyLine() {
        ParsedDeckList resultado = parser.parse("\n\nquantity,name\n4,Sol Ring\n");

        assertThat(resultado.entries()).hasSize(1);
        assertThat(resultado.entries().get(0).name()).isEqualTo("Sol Ring");
    }

    @Test
    void quotedHeaderColumnsAreRecognised() {
        ParsedDeckList resultado = parser.parse("""
                "name","quantity"
                Sol Ring,4
                """);

        assertThat(resultado.entries()).hasSize(1);
        assertThat(resultado.entries().get(0).name()).isEqualTo("Sol Ring");
        assertThat(resultado.entries().get(0).quantity()).isEqualTo(4);
    }

    @Test
    void separatorIsDetectedOutsideQuotedCells() {
        // La coma va ENTRECOMILLADA en la cabecera: no cuenta como separador, el real
        // es ';' (con ',' la cabecera sería de un solo campo y no tendría name)
        ParsedDeckList resultado = parser.parse("""
                "last, name";name
                xx;Sol Ring
                """);

        assertThat(resultado.entries()).hasSize(1);
        assertThat(resultado.entries().get(0).name()).isEqualTo("Sol Ring");
    }

    @Test
    void ignoresRowsWithoutName() {
        ParsedDeckList resultado = parser.parse("""
                quantity,name

                4,Sol Ring
                esto no es una carta
                """);

        assertThat(resultado.entries()).hasSize(1);
        assertThat(resultado.entries().get(0).name()).isEqualTo("Sol Ring");
    }

    @Test
    void defaultsMissingQuantityToOne() {
        // Fila sin valor de quantity y fila sin la columna: ambas se quedan en 1
        ParsedDeckList resultado = parser.parse("""
                name,quantity
                Sol Ring,
                Plains
                """);

        assertThat(resultado.entries()).hasSize(2);
        assertThat(resultado.entries().get(0).quantity()).isEqualTo(1);
        assertThat(resultado.entries().get(1).quantity()).isEqualTo(1);
    }

    @Test
    void throwsWhenQuantityIsNotPositive() {
        assertThatThrownBy(() -> parser.parse("quantity,name\n0,Sol Ring\n"))
                .isInstanceOf(DeckListParseException.class);
    }

    @Test
    void throwsWhenQuantityIsNotANumber() {
        assertThatThrownBy(() -> parser.parse("quantity,name\ncuatro,Sol Ring\n"))
                .isInstanceOf(DeckListParseException.class);
    }

    @Test
    void headerWithoutCards_throws() {
        assertThatThrownBy(() -> parser.parse("quantity,name\n"))
                .isInstanceOf(DeckListParseException.class);
    }

    @Test
    void missingHeader_throws() {
        // Un archivo de solo datos, sin fila de cabecera: nada de lo que hay es una columna conocida
        assertThatThrownBy(() -> parser.parse("Sol Ring,4\nPlains,20\n"))
                .isInstanceOf(DeckListParseException.class);
    }

    @Test
    void headerWithoutNameColumn_throws() {
        assertThatThrownBy(() -> parser.parse("quantity,set\n4,NEO\n"))
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
    void format_returnsCsv() {
        assertThat(parser.format()).isEqualTo(DeckImportFormat.CSV);
    }
}
