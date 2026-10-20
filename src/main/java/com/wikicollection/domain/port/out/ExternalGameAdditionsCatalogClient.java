package com.wikicollection.domain.port.out;

import com.wikicollection.domain.model.GameAdditionsPage;

/**
 * Catálogo externo de adiciones de un juego. Se separa del buscador general a
 * propósito: los DLC no deben ofrecerse como juegos sueltos en la búsqueda, solo
 * a través de su juego base.
 */
public interface ExternalGameAdditionsCatalogClient {

    GameAdditionsPage getAdditions(String externalId);
}
