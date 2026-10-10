package com.wikicollection.domain.port.in;

import com.wikicollection.domain.model.MagicCard;
import com.wikicollection.domain.model.MagicCardPrinting;
import com.wikicollection.domain.model.MagicCardSearchCriteria;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface MagicCardUseCase {

    Page<MagicCard> search(MagicCardSearchCriteria criteria, Pageable pageable);

    Page<MagicCard> search(MagicCardSearchCriteria criteria, Pageable pageable, String owner, String viewerId);

    MagicCard findById(String id);

    MagicCard addFromScryfall(String scryfallId, int quantity, String ownerId);

    /**
     * Todas las impresiones de una carta, para poder elegir cuál guardar.
     *
     * @param scryfallId id de cualquier impresión de la carta; se resuelve al {@code oracle_id}
     * @param page       base 0; Scryfall no permite pedir menos de 175 por página
     */
    Page<MagicCardPrinting> printings(String scryfallId, int page);

    void delete(String id, String userId);

    /**
     * Suma copias a una carta existente de la colección del usuario.
     *
     * @param id       identificador de la carta
     * @param quantity copias a añadir (mínimo 1)
     * @param userId   usuario que realiza la operación; debe ser el dueño
     * @return la carta con la cantidad actualizada
     */
    MagicCard addCopies(String id, int quantity, String userId);
}
