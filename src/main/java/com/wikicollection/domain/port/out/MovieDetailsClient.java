package com.wikicollection.domain.port.out;

import java.util.List;

import com.wikicollection.domain.model.MovieMediaType;

public interface MovieDetailsClient {

    List<String> getGenres(Long tmdbId, MovieMediaType mediaType);
}
