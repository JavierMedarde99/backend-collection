package com.wikicollection.infrastructure.adapter.in.decklist;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

import com.wikicollection.application.exception.DeckListParseException;
import com.wikicollection.domain.model.DeckImportFormat;
import com.wikicollection.domain.model.DeckListEntry;
import com.wikicollection.domain.model.ParsedDeckList;

class MtgoTextDeckListParserTest {

    private static final String EJEMPLO = """
            // deck comment
            COMMANDER
            1 Atraxa, Grand Unifier (ONE) 32

            DECK
            4 Sol Ring (NEO) 269
            2 Plains
            1 Not A Real Card (XXX) 7 *F*  $1.23

            SIDEBOARD
            2 Negate (M21) 68
            """;

    private final MtgoTextDeckListParser parser = new MtgoTextDeckListParser();

    @Test
    void parsesCommanderAndDeckZones() {
        ParsedDeckList resultado = parser.parse(EJEMPLO);

        assertThat(resultado.entries()).hasSize(4);

        DeckListEntry comandante = resultado.entries().get(0);
        assertThat(comandante.name()).isEqualTo("Atraxa, Grand Unifier");
        assertThat(comandante.commander()).isTrue();
        assertThat(comandante.line()).isEqualTo(3);

        DeckListEntry solRing = resultado.entries().get(1);
        assertThat(solRing.quantity()).isEqualTo(4);
        assertThat(solRing.setCode()).isEqualTo("NEO");
        assertThat(solRing.collectorNumber()).isEqualTo("269");

        DeckListEntry inventada = resultado.entries().get(3);
        assertThat(inventada.name()).isEqualTo("Not A Real Card");
        assertThat(inventada.commander()).isFalse();

        assertThat(resultado.sideboardIgnored()).isEqualTo(1);
    }

    @Test
    void ignoresCommentLinesAndBlankLines() {
        ParsedDeckList resultado = parser.parse(EJEMPLO);

        assertThat(resultado.entries())
                .extracting(DeckListEntry::name)
                .doesNotContain("// deck comment")
                .allSatisfy(nombre -> assertThat(nombre).isNotBlank());
    }

    @Test
    void parsesEntryWithoutSetOrNumber() {
        ParsedDeckList resultado = parser.parse("4 Lightning Bolt");

        assertThat(resultado.entries()).hasSize(1);
        DeckListEntry entrada = resultado.entries().get(0);
        assertThat(entrada.quantity()).isEqualTo(4);
        assertThat(entrada.name()).isEqualTo("Lightning Bolt");
        assertThat(entrada.setCode()).isNull();
        assertThat(entrada.collectorNumber()).isNull();
    }

    @Test
    void lowercasedZoneHeadersAreRecognized() {
        ParsedDeckList resultado = parser.parse("""
                commander
                1 Atraxa, Grand Unifier

                deck
                4 Sol Ring
                """);

        assertThat(resultado.entries()).hasSize(2);
        assertThat(resultado.entries().get(0).commander()).isTrue();
        assertThat(resultado.entries().get(1).commander()).isFalse();
        assertThat(resultado.sideboardIgnored()).isZero();
    }

    @Test
    void emptyContent_throws() {
        assertThatThrownBy(() -> parser.parse("   \n\n  "))
                .isInstanceOf(DeckListParseException.class);
    }

    @Test
    void contentWithoutAnyCardLine_throws() {
        assertThatThrownBy(() -> parser.parse("COMMANDER\n"))
                .isInstanceOf(DeckListParseException.class);
    }

    @Test
    void throwsWhenQuantityIsNotPositive() {
        assertThatThrownBy(() -> parser.parse("DECK\n0 Sol Ring"))
                .isInstanceOf(DeckListParseException.class);
    }

    @Test
    void throwsWhenQuantityDoesNotFitInInt() {
        assertThatThrownBy(() -> parser.parse("DECK\n2147483648 Sol Ring"))
                .isInstanceOf(DeckListParseException.class);
    }

    @Test
    void ignoresLinesThatAreNotCards() {
        ParsedDeckList resultado = parser.parse("""
                DECK
                4 Sol Ring
                Mazo de prueba para el Friday Night
                1 Plains
                """);

        assertThat(resultado.entries())
                .extracting(DeckListEntry::name)
                .containsExactly("Sol Ring", "Plains");
        assertThat(resultado.sideboardIgnored()).isZero();
    }

    @Test
    void zoneHeaderAfterUtf8BomIsRecognized() {
        ParsedDeckList resultado = parser.parse("\uFEFFCOMMANDER\n1 Atraxa, Grand Unifier\n");

        assertThat(resultado.entries()).hasSize(1);
        assertThat(resultado.entries().get(0).commander()).isTrue();
    }

    @Test
    void format_returnsTxt() {
        assertThat(parser.format()).isEqualTo(DeckImportFormat.TXT);
    }
}
