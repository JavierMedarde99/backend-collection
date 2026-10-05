package com.wikicollection.infrastructure.adapter.in.decklist;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.wikicollection.domain.model.DeckImportFormat;

import org.junit.jupiter.api.Test;

/**
 * Deducir el formato de un archivo de lista de mazo (#353).
 *
 * <p>La extensión manda cuando la hay. Si no, se deduce del contenido; y si no hay ni una
 * cosa ni la otra, se asume texto plano porque es lo que exportan Arena, Moxfield y Deckbox.
 *
 * <p>Lo que se prueba aquí sobre todo es {@code requireConsistent}, que es donde está el
 * riesgo: un formato mal declarado que se acepta produce un job en FAILED mucho después, sin
 * más explicación que "no se pudo parsear", cuando el problema era una etiqueta equivocada.
 */
class DeckImportFormatDetectorTest {

    private static final String TXT = "COMMANDER\n1 Sol Ring\n\n4 Counterspell\n";
    private static final String JSON = "{\"cards\":[{\"name\":\"Sol Ring\",\"quantity\":4}]}";
    private static final String CSV = "quantity,name,commander\n4,Sol Ring,false\n";

    private final DeckImportFormatDetector detector = new DeckImportFormatDetector();

    // ------------------------------------------------------------------ deducir

    @Test
    void detect_prefersTheExtension() {
        assertThat(detector.detect(JSON, "deck.csv")).isEqualTo(DeckImportFormat.CSV);
        assertThat(detector.detect(TXT, "deck.json")).isEqualTo(DeckImportFormat.JSON);
        assertThat(detector.detect(TXT, "deck.txt")).isEqualTo(DeckImportFormat.TXT);
    }

    @Test
    void detect_ignoresTheExtensionCase() {
        assertThat(detector.detect(TXT, "DECK.CSV")).isEqualTo(DeckImportFormat.CSV);
    }

    @Test
    void detect_fallsBackToContentWhenThereIsNoUsefulExtension() {
        assertThat(detector.detect(JSON, null)).isEqualTo(DeckImportFormat.JSON);
        assertThat(detector.detect(JSON, "deck")).isEqualTo(DeckImportFormat.JSON);
        assertThat(detector.detect(JSON, "deck.xlsx")).isEqualTo(DeckImportFormat.JSON);
        assertThat(detector.detect(CSV, "lista")).isEqualTo(DeckImportFormat.CSV);
    }

    @Test
    void detect_assumesTextWhenNothingSaysOtherwise() {
        assertThat(detector.detect(TXT, null)).isEqualTo(DeckImportFormat.TXT);
        assertThat(detector.detect("   ", null)).isEqualTo(DeckImportFormat.TXT);
    }

    @Test
    void detect_doesNotMistakeACardLineForACsvHeader() {
        // "4 Sol Ring" lleva coma y quizá nombre, pero no es una cabecera.
        assertThat(detector.detect("4 Sol Ring, unOwned\n", null)).isEqualTo(DeckImportFormat.TXT);
    }

    // -------------------------------------------------------------- consistencia

    @Test
    void requireConsistent_passesWhenTheContentMatches() {
        detector.requireConsistent(DeckImportFormat.JSON, JSON, null);
        detector.requireConsistent(DeckImportFormat.TXT, TXT, "deck.txt");
        detector.requireConsistent(DeckImportFormat.CSV, CSV, "deck.csv");
    }

    @Test
    void requireConsistent_passesWhenNothingIsDeclared() {
        detector.requireConsistent(null, JSON, null);
    }

    @Test
    void requireConsistent_throwsWhenTheContentSaysOtherwise() {
        assertThatThrownBy(() -> detector.requireConsistent(DeckImportFormat.JSON, TXT, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("JSON")
                .hasMessageContaining("TXT");
    }

    @Test
    void requireConsistent_throwsWhenTheExtensionSaysOtherwise() {
        assertThatThrownBy(() -> detector.requireConsistent(DeckImportFormat.JSON, TXT, "deck.csv"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("deck.csv")
                .hasMessageContaining("CSV");
    }

    /**
     * Sin marcas de JSON ni de CSV, el archivo es de texto plano aunque el cliente diga otra
     * cosa: dejarlo pasar convertiría un 400 en un job en FAILED sin explicación.
     */
    @Test
    void requireConsistent_treatsMarklessContentAsText() {
        assertThatThrownBy(() -> detector.requireConsistent(DeckImportFormat.CSV, "Sol Ring", null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("TXT");
    }
}