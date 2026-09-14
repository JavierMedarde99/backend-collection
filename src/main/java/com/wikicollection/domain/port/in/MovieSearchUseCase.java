package com.wikicollection.domain.port.in;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.wikicollection.domain.model.MovieMediaType;
import com.wikicollection.domain.model.MovieSearchResult;

public interface MovieSearchUseCase {

    Page<MovieSearchResult> search(String query, Pageable pageable);

    Page<MovieSearchResult> search(String query, MovieMediaType mediaType, Pageable pageable);
}
