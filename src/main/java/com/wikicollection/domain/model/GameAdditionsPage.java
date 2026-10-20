package com.wikicollection.domain.model;

import java.util.List;

/**
 * Página de adiciones (DLC, expansiones...) de un juego tal y como las expone el
 * catálogo externo. {@code totalAvailable} es el total que RAWG reporta para el
 * juego, aunque solo se traiga la primera página: es lo que el cliente necesita
 * para decir «RAWG conoce N adiciones, aquí tienes las 40 primeras».
 */
public record GameAdditionsPage(List<GameSearchResult> additions, int totalAvailable) {

    public static GameAdditionsPage empty() {
        return new GameAdditionsPage(List.of(), 0);
    }
}
