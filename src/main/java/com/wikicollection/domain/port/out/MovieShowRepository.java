package com.wikicollection.domain.port.out;

import java.util.List;
import java.util.Optional;

import com.wikicollection.domain.model.MovieSearchCriteria;
import com.wikicollection.domain.model.MovieShow;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface MovieShowRepository {

    MovieShow save(MovieShow movieShow);

    Optional<MovieShow> findById(String id);

    Page<MovieShow> findAll(Pageable pageable);

    Page<MovieShow> findByCriteria(MovieSearchCriteria criteria, Pageable pageable);

    void deleteById(String id);

    Optional<MovieShow> findByExternalId(String externalId);

    long count();
}
