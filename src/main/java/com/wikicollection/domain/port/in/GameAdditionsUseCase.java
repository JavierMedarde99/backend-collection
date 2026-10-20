package com.wikicollection.domain.port.in;

import java.util.List;

import com.wikicollection.domain.model.GameAdditionsAddResult;
import com.wikicollection.domain.model.GameAdditionsCatalog;
import com.wikicollection.domain.model.GameStatus;

public interface GameAdditionsUseCase {

    /**
     * Catálogo de adiciones de un juego base, con los flags «ya lo tienes» y «ya
     * cuelga de este base» calculados para el espectador.
     *
     * <p>404 si el base no existe, 422 si el base no viene de RAWG, 502 si RAWG falla.
     */
    GameAdditionsCatalog additionsOf(String baseId, String viewerId);

    /**
     * Da de alta adiciones enlazándolas a su base: las que no tenías se crean en
     * el estado pedido, las que ya tenías sueltas se enlazan y las que ya colgaban
     * de este base se saltan. Idempotente en esas dos últimas.
     *
     * <p>400 si la lista de ids es inválida, 404 si el base no existe, 403 si es de
     * otro usuario, 422 si la regla de negocio no se cumple, 502 si RAWG falla.
     */
    GameAdditionsAddResult addAdditions(String baseId, List<String> externalIds, GameStatus status, String userId);
}
