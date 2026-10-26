package com.wikicollection.infrastructure.adapter.out.persistence;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Salida del sub-pipeline {@code total} del {@code $facet}. */
@Getter
@Setter
@NoArgsConstructor
public class GameGroupCountResult {

    private Long count;
}
