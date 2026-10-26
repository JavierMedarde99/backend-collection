package com.wikicollection.infrastructure.adapter.out.persistence;

import java.util.List;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Documento único que devuelve el {@code $facet}: la página de grupos en {@code content} y el
 * total de grupos en {@code total} ({@code [{$count: "count"}]}, vacío si no hay ninguno).
 */
@Getter
@Setter
@NoArgsConstructor
public class GameGroupFacetResult {

    private List<GameGroupAggregationResult> content;

    private List<GameGroupCountResult> total;
}
