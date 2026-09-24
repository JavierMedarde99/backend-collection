package com.wikicollection.application.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import com.wikicollection.application.exception.MovieShowConflictException;
import com.wikicollection.application.exception.MovieShowNotFoundException;
import com.wikicollection.domain.model.MovieSearchCriteria;
import com.wikicollection.domain.model.MovieShow;
import com.wikicollection.domain.port.in.MovieShowUseCase;
import com.wikicollection.domain.port.out.MovieShowRepository;

import org.springframework.dao.DuplicateKeyException;
import com.wikicollection.domain.model.CollectionType;
import com.wikicollection.domain.model.MovieMediaType;
import com.wikicollection.domain.model.ProviderAccessType;
import com.wikicollection.domain.model.StreamingProvider;
import com.wikicollection.domain.model.TmdbWatchProvider;
import com.wikicollection.domain.port.out.WatchProvidersClient;
import com.wikicollection.infrastructure.adapter.out.tmdb.ProviderUrlMapper;

import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
public class MovieShowService implements MovieShowUseCase {

    private static final String WATCH_COUNTRY = "ES";

    private final MovieShowRepository movieShowRepository;
    private final DateRangeValidator dateRangeValidator;
    private final OwnershipValidator ownershipValidator;

    private final OwnerScopeResolver ownerScopeResolver;

    private final OwnerResolver ownerResolver;

    private final WatchProvidersClient watchProvidersClient;
    private final ProviderUrlMapper providerUrlMapper;

    public MovieShowService(MovieShowRepository movieShowRepository,
                            DateRangeValidator dateRangeValidator,
                            OwnershipValidator ownershipValidator,
                       OwnerScopeResolver ownerScopeResolver,
                       OwnerResolver ownerResolver,
                       WatchProvidersClient watchProvidersClient,
                       ProviderUrlMapper providerUrlMapper) {
        this.movieShowRepository = movieShowRepository;
        this.dateRangeValidator = dateRangeValidator;
        this.ownershipValidator = ownershipValidator;
        this.ownerResolver = ownerResolver;
        this.ownerScopeResolver = ownerScopeResolver;
        this.watchProvidersClient = watchProvidersClient;
        this.providerUrlMapper = providerUrlMapper;
    }

    @Override
    public Page<MovieShow> search(MovieSearchCriteria criteria, Pageable pageable) {
        return movieShowRepository.findByCriteria(criteria, pageable);
    }

    @Override
    public Page<MovieShow> search(MovieSearchCriteria criteria, Pageable pageable, String owner, String viewerId) {
        OwnerScopeResolver.Scope scope = ownerScopeResolver.resolve(CollectionType.MOVIESHOWS, owner, viewerId);
        return movieShowRepository.findByCriteria(new MovieSearchCriteria(criteria.name(), criteria.status(), criteria.mediaType(), scope.ownerId(), scope.excludeOwnerIds()), pageable);
    }

    @Override
    public MovieShow findById(String id) {
        return movieShowRepository.findById(id)
                .orElseThrow(() -> new MovieShowNotFoundException("Película/serie no encontrada con id: " + id));
    }

    @Override
    @Transactional
    public MovieShow save(MovieShow movieShow, String ownerId) {
        movieShow.setOwnerId(ownerId);
        movieShow.setUserOwned(ownerResolver.resolveOwner(ownerId));
        dateRangeValidator.validate(movieShow.getDateAdded(), movieShow.getDateCompleted());
        assertNoDuplicate(movieShow.getExternalId(), null);
        enrichStreamingProviders(movieShow);
        return saveOrConflict(movieShow);
    }

    @Override
    @Transactional
    public MovieShow update(String id, MovieShow updates, String userId) {
        MovieShow existing = findById(id);
        ownershipValidator.validateOwner(existing.getOwnerId(), userId);
        copyUpdatableFields(existing, updates);
        enrichStreamingProviders(existing);
        dateRangeValidator.validate(existing.getDateAdded(), existing.getDateCompleted());
        assertNoDuplicate(existing.getExternalId(), id);
        return saveOrConflict(existing);
    }

    private MovieShow saveOrConflict(MovieShow movieShow) {
        try {
            return movieShowRepository.save(movieShow);
        } catch (DuplicateKeyException e) {
            throw new MovieShowConflictException(
                    "Ya existe una película/serie con externalId: " + movieShow.getExternalId());
        }
    }

    @Override
    @Transactional
    public MovieShow refreshStreamingProviders(String id, String userId) {
        MovieShow existing = findById(id);
        ownershipValidator.validateOwner(existing.getOwnerId(), userId);
        existing.setStreamingProviders(null);
        existing.setWatchCountry(null);
        enrichStreamingProviders(existing);
        return saveOrConflict(existing);
    }

    @Override
    public void delete(String id, String userId) {
        MovieShow existing = findById(id);
        ownershipValidator.validateOwner(existing.getOwnerId(), userId);
        movieShowRepository.deleteById(id);
    }

    private void assertNoDuplicate(String externalId, String currentId) {
        if (externalId == null || externalId.isBlank()) {
            return;
        }
        movieShowRepository.findByExternalId(externalId)
                .filter(existing -> !Objects.equals(existing.getId(), currentId))
                .ifPresent(existing -> {
                    throw new MovieShowConflictException(
                            "Ya existe una película/serie con externalId: " + externalId);
                });
    }

    private void copyUpdatableFields(MovieShow target, MovieShow source) {
        target.setExternalId(source.getExternalId());
        target.setTitle(source.getTitle());
        target.setOverview(source.getOverview());
        target.setReleaseDate(source.getReleaseDate());
        target.setPosterUrl(source.getPosterUrl());
        target.setBackdropUrl(source.getBackdropUrl());
        target.setVoteAverage(source.getVoteAverage());
        target.setMediaType(source.getMediaType());
        target.setStatus(source.getStatus());
        target.setUserRating(source.getUserRating());
        target.setComment(source.getComment());
        target.setDateAdded(source.getDateAdded());
        target.setDateCompleted(source.getDateCompleted());
        target.setExternalSource(source.getExternalSource());
        target.setStreamingProviders(source.getStreamingProviders());
        target.setWatchCountry(source.getWatchCountry());
    }

    private void enrichStreamingProviders(MovieShow movieShow) {
        Long tmdbId = parseTmdbId(movieShow.getExternalId());
        MovieMediaType mediaType = movieShow.getMediaType();
        if (tmdbId == null || mediaType == null) {
            return;
        }
        Map<ProviderAccessType, List<TmdbWatchProvider>> providers;
        try {
            providers = watchProvidersClient.getWatchProviders(tmdbId, mediaType, WATCH_COUNTRY);
        } catch (RuntimeException e) {
            log.warn("Proveedores no disponibles para {}: {}", tmdbId, e.getMessage());
            return;
        }
        if (providers == null) {
            return;
        }
        List<StreamingProvider> flattened = new ArrayList<>();
        for (Map.Entry<ProviderAccessType, List<TmdbWatchProvider>> entry : providers.entrySet()) {
            if (entry.getValue() == null) {
                continue;
            }
            for (TmdbWatchProvider provider : entry.getValue()) {
                flattened.add(StreamingProvider.builder()
                        .providerId(provider.providerId())
                        .providerName(provider.providerName())
                        .logoUrl(providerUrlMapper.buildLogoUrl(provider.logoPath()))
                        .type(entry.getKey())
                        .build());
            }
        }
        if (!flattened.isEmpty()) {
            movieShow.setStreamingProviders(flattened);
            movieShow.setWatchCountry(WATCH_COUNTRY);
        }
    }

    private static Long parseTmdbId(String externalId) {
        if (externalId == null || externalId.isBlank()) {
            return null;
        }
        try {
            return Long.valueOf(externalId.strip());
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
