package com.wikicollection.domain.port.out;

import java.util.List;

import com.wikicollection.domain.model.MagicCard;
import com.wikicollection.domain.model.MagicCardPrinting;
import com.wikicollection.domain.model.MagicCardSearchResult;

import org.springframework.data.domain.Page;

public interface ExternalMagicCardCatalogClient {

    List<MagicCardSearchResult> search(String query);

    List<MagicCardSearchResult> searchCommanders(String colors);

    MagicCard findById(String id);

    MagicCard findByName(String name);

    /**
     * Busca una carta por su nombre exacto, para la importación de mazos.
     *
     * <p>Usa {@code unique=oracle}: sin él Scryfall devuelve una fila por reimpresión y las
     * veinte reimpresiones de "Sol Ring" harían que cada línea del mazo pareciera ambigua.
     *
     * <p>A diferencia de {@link #search(String)}, un fallo de Scryfall se propaga en vez de
     * convertirse en una lista vacía: aquí el vacío significa "esta carta no existe", y un
     * 500 disfrazado de vacío haría que la importación guardara el mazo sin esa carta y le
     * dijera al usuario que no existe.
     */
    List<MagicCardSearchResult> searchByNameExact(String name);

    /**
     * Cartas parecidas a un nombre, como sugerencias cuando la búsqueda exacta no encuentra
     * nada. También con {@code unique=oracle} y sin parámetros de paginación, que Scryfall
     * ignora en silencio; el recorte de resultados es del cliente.
     */
    List<MagicCardSearchResult> searchSuggestions(String name);

    /**
     * Todas las impresiones de una carta, ordenadas por fecha de lanzamiento descendente.
     * La búsqueda de Scryfall es fija de 175 por página, así que {@code page} es base 0
     * y cada página trae hasta {@code 175} elementos.
     *
     * @param oracleId identificador de la carta a través de todas sus reimpresiones
     */
    Page<MagicCardPrinting> findPrintings(String oracleId, int page);
}
