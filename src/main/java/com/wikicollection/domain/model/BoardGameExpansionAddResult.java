package com.wikicollection.domain.model;

import java.util.List;

/**
 * Resultado de {@code POST /boardgames/{id}/expansions}: lo creado y lo que ya
 * estaba en la colección (el alta es idempotente, nunca un error).
 */
public record BoardGameExpansionAddResult(
        List<BoardGame> created,
        List<BoardGameExpansionSkip> skipped) {
}
