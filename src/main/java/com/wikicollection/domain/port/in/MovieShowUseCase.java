package com.wikicollection.domain.port.in;

import com.wikicollection.domain.model.MovieSearchCriteria;
import com.wikicollection.domain.model.MovieShow;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface MovieShowUseCase {

    Page<MovieShow> search(MovieSearchCriteria criteria, Pageable pageable);

    MovieShow findById(String id);

    MovieShow save(MovieShow movieShow);

    MovieShow update(String id, MovieShow updates);

    void delete(String id);
}
