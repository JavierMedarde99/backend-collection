package com.wikicollection.application.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;

/**
 * Cómo se comparan los nombres de carta durante la importación (#353).
 *
 * <p>La normalización perdona acentos, mayúsculas y espacios repetidos, y nada más. Ese
 * límite es el que separa "Kóngou" de "Kongou" (la misma carta) de "Llanuras" y "Plains"
 * (dos cartas distintas): ampliarlo haría que la importación metiera cartas que el usuario no
 * pidió, sin avisar.
 */
class DeckNameNormalizerTest {

    private final DeckNameNormalizer normalizer = new DeckNameNormalizer();

    @Test
    void normalize_lowercasesAndTrims() {
        assertThat(normalizer.normalize("  SOL RING  ")).isEqualTo("sol ring");
    }

    @Test
    void normalize_dropsAccents() {
        assertThat(normalizer.normalize("Kóngou, Keeper of the Deep"))
                .isEqualTo("kongou, keeper of the deep");
        assertThat(normalizer.normalize("Atraxa, Gran Unificadora")).isEqualTo("atraxa, gran unificadora");
    }

    @Test
    void normalize_collapsesRepeatedWhitespace() {
        assertThat(normalizer.normalize("Sol    Ring")).isEqualTo("sol ring");
    }

    @Test
    void normalize_survivesNullAndBlank() {
        assertThat(normalizer.normalize(null)).isEmpty();
        assertThat(normalizer.normalize("   ")).isEmpty();
    }

    /**
     * Solo las cinco tierras básicas. El resto (también las duales y Wasteland) tienen que
     * ir a Scryfall: si se adivinaran aquí se les asignaría un {@code typeLine} de mentira y
     * {@code DeckValidator} las exceptuaría de la regla de singleton.
     */
    @Test
    void isBasicLand_recognizesOnlyTheFiveBasicLands() {
        assertThat(normalizer.isBasicLand("Plains")).isTrue();
        assertThat(normalizer.isBasicLand("plains")).isTrue();
        assertThat(normalizer.isBasicLand("Island")).isTrue();
        assertThat(normalizer.isBasicLand("Forest")).isTrue();
        assertThat(normalizer.isBasicLand("Underground Sea")).isFalse();
        assertThat(normalizer.isBasicLand("Llanowar Elves")).isFalse();
        assertThat(normalizer.isBasicLand("Wasteland")).isFalse();
        // Se resuelven contra Scryfall con su type_line real, no se inventan.
        assertThat(normalizer.isBasicLand("Snow-Covered Plains")).isFalse();
        assertThat(normalizer.isBasicLand("Wastes")).isFalse();
    }

    @Test
    void isBasicLand_saysNoToEverythingElse() {
        assertThat(normalizer.isBasicLand("Sol Ring")).isFalse();
        assertThat(normalizer.isBasicLand("Exotic Forest")).isFalse();
        assertThat(normalizer.isBasicLand(null)).isFalse();
    }

    @Test
    void normalizeAll_dropsBlanksAndDuplicatesKeepingOrder() {
        assertThat(normalizer.normalizeAll(Arrays.asList("Sol Ring", "sol ring", "  ", null, "Plains")))
                .containsExactly("sol ring", "plains");
    }

    @Test
    void normalizeAll_survivesNull() {
        assertThat(normalizer.normalizeAll(null)).isEmpty();
    }
}