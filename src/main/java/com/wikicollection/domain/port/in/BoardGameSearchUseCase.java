package com.wikicollection.domain.port.in;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.wikicollection.domain.model.BoardGameSearchResult;

public interface BoardGameSearchUseCase {

    Page<BoardGameSearchResult> search(String query, Pageable pageable);
}
