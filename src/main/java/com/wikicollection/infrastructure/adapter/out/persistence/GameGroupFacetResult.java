package com.wikicollection.infrastructure.adapter.out.persistence;

import java.util.List;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class GameGroupFacetResult {
    private List<GameGroupAggregationResult> content;
    private List<GameGroupCountResult> total;
}
