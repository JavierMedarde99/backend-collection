package com.wikicollection.domain.model;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;

class BookSearchCriteriaSeriesTest {

    @Test
    void sinSerieNoHayFiltroDeSerie() {
        BookSearchCriteria criteria = new BookSearchCriteria(null, null, null, null, null, null, null);
        assertThat(criteria.hasSeries()).isFalse();
    }

    @Test
    void conSerieEnBlancoNoHayFiltroDeSerie() {
        BookSearchCriteria criteria = new BookSearchCriteria(null, null, null, null, null, null, null, "   ");
        assertThat(criteria.hasSeries()).isFalse();
    }

    @Test
    void conSerieSiHayFiltroDeSerie() {
        BookSearchCriteria criteria = new BookSearchCriteria(null, null, null, null, null, null, null, "Harry Potter");
        assertThat(criteria.hasSeries()).isTrue();
        assertThat(criteria.series()).isEqualTo("Harry Potter");
    }
}