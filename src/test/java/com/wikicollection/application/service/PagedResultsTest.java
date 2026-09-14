package com.wikicollection.application.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.stream.IntStream;

import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

class PagedResultsTest {

    private final List<String> all = IntStream.range(0, 25).mapToObj(i -> "item-" + i).toList();

    @Test
    void slice_firstPage() {
        var page = PagedResults.slice(all, PageRequest.of(0, 10));

        assertThat(page.getContent()).isEqualTo(all.subList(0, 10));
        assertThat(page.getTotalElements()).isEqualTo(25);
        assertThat(page.getTotalPages()).isEqualTo(3);
        assertThat(page.getNumber()).isZero();
    }

    @Test
    void slice_lastPartialPage() {
        var page = PagedResults.slice(all, PageRequest.of(2, 10));

        assertThat(page.getContent()).isEqualTo(all.subList(20, 25));
        assertThat(page.getTotalElements()).isEqualTo(25);
    }

    @Test
    void slice_outOfRangePage_returnsEmpty() {
        var page = PagedResults.slice(all, PageRequest.of(9, 10));

        assertThat(page.getContent()).isEmpty();
        assertThat(page.getTotalElements()).isEqualTo(25);
    }

    @Test
    void slice_nullList_returnsEmptyPage() {
        var page = PagedResults.slice(null, PageRequest.of(0, 10));

        assertThat(page.getContent()).isEmpty();
        assertThat(page.getTotalElements()).isZero();
    }

    @Test
    void slice_unpaged_returnsAll() {
        var page = PagedResults.slice(all, Pageable.unpaged());

        assertThat(page.getContent()).isEqualTo(all);
    }
}
