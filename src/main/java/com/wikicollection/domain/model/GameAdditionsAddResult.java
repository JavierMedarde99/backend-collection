package com.wikicollection.domain.model;

import java.util.List;

/**
 * Resultado de {@code POST /games/{id}/additions}: los DLC creados, los juegos
 * sueltos que ya eran tuyos y se han enlazado al base (sin perder nada suyo) y
 * los que ya colgaban de este base (el alta es idempotente, nunca un error).
 */
public record GameAdditionsAddResult(
        List<Game> created,
        List<Game> linked,
        List<GameAdditionSkip> skipped) {
}
