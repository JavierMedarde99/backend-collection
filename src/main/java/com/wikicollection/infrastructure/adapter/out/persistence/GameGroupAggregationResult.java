package com.wikicollection.infrastructure.adapter.out.persistence;

import java.util.List;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class GameGroupAggregationResult {
    private String gameKey;
    private String title;
    private String thumbnailUrl;
    private List<GameEntity> games;
}
