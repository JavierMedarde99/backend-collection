package com.wikicollection.infrastructure.adapter.out.persistence;

import java.util.List;

import org.springframework.data.annotation.Id;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Una fila del {@code $group} de la búsqueda agrupada: {@code _id} es la {@code gameKey} y
 * {@code games} la lista de registros (uno por plataforma) que comparten esa clave.
 */
@Getter
@Setter
@NoArgsConstructor
public class GameGroupAggregationResult {

    @Id
    private String gameKey;

    private String title;

    private String thumbnailUrl;

    private List<GameEntity> games;
}
