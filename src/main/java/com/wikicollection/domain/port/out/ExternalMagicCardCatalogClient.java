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
     * Todas las impresiones de una carta, ordenadas por fecha de lanzamiento descendente.
     * La búsqueda de Scryfall es fija de 175 por página, así que {@code page} es base 0
     * y cada página trae hasta {@code 175} elementos.
     *
     * @param oracleId identificador de la carta a través de todas sus reimpresiones
     */
    Page<MagicCardPrinting> findPrintings(String oracleId, int page);
}
