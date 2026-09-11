package com.wikicollection.domain.port.out;

import java.util.List;

import com.wikicollection.domain.model.MovieMediaType;
import com.wikicollection.domain.model.MovieSearchResult;

public interface ExternalMovieCatalogClient {

    List<MovieSearchResult> search(String query);

    List<MovieSearchResult> search(String query, MovieMediaType mediaType);
}
