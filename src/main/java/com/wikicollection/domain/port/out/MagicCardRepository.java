package com.wikicollection.domain.port.out;

import java.util.List;
import java.util.Optional;

import com.wikicollection.domain.model.MagicCard;
import com.wikicollection.domain.model.MagicCardSearchCriteria;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface MagicCardRepository {

    Page<MagicCard> search(MagicCardSearchCriteria criteria, Pageable pageable);

    Optional<MagicCard> findById(String id);

    MagicCard save(MagicCard magicCard);

    void deleteById(String id);

    void updateOwnerName(String ownerId, String ownerName);

    /**
     * Nombres distintos de las cartas que el dueño tiene en su colección, en una sola
     * consulta.
     *
     * <p>La importación de mazos (#353) necesita saber, para cada carta del mazo, si el
     * dueño ya la tiene. Hacerlo carta a carta con una búsqueda por nombre compila un regex
     * sobre {@code name}, que no está indexado: un mazo de 99 cartas serían 99 escaneos.
     * Aquí se aprovecha el índice de {@code ownerId}, que sí existe.
     *
     * @return nombres distintos, sin vacíos; lista vacía si el dueño no tiene cartas
     */
    List<String> findNamesByOwnerId(String ownerId);
}
