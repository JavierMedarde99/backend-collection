package com.wikicollection.domain.port.in;

import java.util.List;

import com.wikicollection.domain.model.MovieSearchResult;

public interface MovieSearchUseCase {

    List<MovieSearchResult> search(String query);
}
