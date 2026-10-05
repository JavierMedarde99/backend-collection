package com.wikicollection.application.service;

import java.text.Normalizer;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import org.springframework.stereotype.Component;

/**
 * Cómo se comparan y se agrupan los nombres de carta durante la importación (#353).
 *
 * <p>Scryfall indexa en inglés y con su propia puntuación, así que el nombre de un archivo y
 * el de la respuesta no siempre son idénticos character a character. La normalización solo
 * perdona lo que no cambia de carta a carta: acentos, mayúsculas y espacios repetidos. Nada
 * más, porque cualquier otra tolerancia acabaría decidiendo sola que "Llanuras" es "Plains".
 */
@Component
public class DeckNameNormalizer {

    /**
     * Tierras básicas. No hacen falta ni id de Scryfall ni búsqueda para meterlas en un mazo,
     * y su {@code type_line} es lo que {@code DeckValidator} usa para exceptuarlas de la
     * regla de singleton (salvo una, un singleton básico es siempre un error del archivo).
     *
     * <p>Solo las que se ven en mazos Commander. Las tierras básicas duplicadas que sí
     * existen en Scryfall (Tawny Port, elustered City) no están: para esas la búsqueda
     * funciona igual, solo que es una llamada de más.
     */
    private static final Set<String> BASIC_LANDS = Set.of(
            "plains", "island", "swamp", "mountain", "forest",
            "wasteland", "dryad arbor",
            "arctic plains", "badlands", "bayou", "coastal plains", "jungle",
            "llanowar elves", "scrubland", "tundra", "underground sea", "volcanic island");

    /**
     * Forma de comparar dos nombres de carta.
     *
     * <p>Minúsculas, sin acentos y con los espacios colapsados: "Kóngou, Keeper of the Deep"
     * y "Kongou, Keeper of the Deep" son la misma carta, "Llanuras" y "Plains" no.
     */
    public String normalize(String name) {
        if (name == null) {
            return "";
        }
        String stripped = Normalizer.normalize(name, Normalizer.Form.NFD)
                .replaceAll("\\p{Mn}+", "");
        return stripped.toLowerCase(Locale.ROOT).replaceAll("\\s+", " ").trim();
    }

    /**
     * Si el nombre es el de una tierra básica, que entra en el mazo sin preguntar a nadie.
     */
    public boolean isBasicLand(String name) {
        return BASIC_LANDS.contains(normalize(name));
    }

    /**
     * Normaliza una lista de una vez, para no tener que hacerlo carta a carta al comparar
     * el mazo con la colección del dueño.
     *
     * @return los nombres normalizados, sin vacíos y sin duplicados, en orden de aparición
     */
    public Set<String> normalizeAll(List<String> names) {
        Set<String> normalized = new LinkedHashSet<>();
        if (names == null) {
            return normalized;
        }
        for (String name : names) {
            String value = normalize(name);
            if (!value.isEmpty()) {
                normalized.add(value);
            }
        }
        return normalized;
    }
}