package com.wikicollection.domain.port.out;

import java.util.List;
import java.util.Map;

import com.wikicollection.domain.model.MovieMediaType;
import com.wikicollection.domain.model.ProviderAccessType;
import com.wikicollection.domain.model.TmdbWatchProvider;

public interface WatchProvidersClient {

    Map<ProviderAccessType, List<TmdbWatchProvider>> getWatchProviders(
            Long tmdbId, MovieMediaType mediaType, String country);
}
