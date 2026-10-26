package com.wikicollection.domain.model;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;

class GameSearchCriteriaTest {

    @Test
    void sixArgumentConstructor_defaultsToFlatSearch() {
        GameSearchCriteria criteria = new GameSearchCriteria("hollow", "PC", null, List.of(), "u1", List.of());

        assertThat(criteria.groupByGame()).isFalse();
    }

    @Test
    void flatAndGroupedCriteria_areNotEqual_soCacheKeysDiffer() {
        GameSearchCriteria flat = new GameSearchCriteria(null, null, null, null, null, null, false);
        GameSearchCriteria grouped = new GameSearchCriteria(null, null, null, null, null, null, true);

        assertThat(flat).isNotEqualTo(grouped);
        assertThat(flat.hashCode()).isNotEqualTo(grouped.hashCode());
    }
}
