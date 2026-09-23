package com.wikicollection.domain.port.in;

import com.wikicollection.domain.model.MovieSearchCriteria;
import com.wikicollection.domain.model.MovieShow;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface MovieShowUseCase {

    Page<MovieShow> search(MovieSearchCriteria criteria, Pageable pageable);

    Page<MovieShow> search(MovieSearchCriteria criteria, Pageable pageable, String owner, String viewerId);

    MovieShow findById(String id);

    MovieShow save(MovieShow movieShow, String ownerId);

    MovieShow update(String id, MovieShow updates, String userId);

    MovieShow refreshStreamingProviders(String id, String userId);

    void delete(String id, String userId);
}
