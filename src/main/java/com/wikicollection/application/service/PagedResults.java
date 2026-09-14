package com.wikicollection.application.service;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

final class PagedResults {

    private PagedResults() {
    }

    static <T> Page<T> slice(List<T> all, Pageable pageable) {
        List<T> content = all == null ? List.of() : all;
        if (!pageable.isPaged()) {
            return new PageImpl<>(content);
        }
        int total = content.size();
        int start = (int) Math.min(pageable.getOffset(), total);
        int end = (int) Math.min((long) start + pageable.getPageSize(), total);
        return new PageImpl<>(content.subList(start, end), pageable, total);
    }
}
