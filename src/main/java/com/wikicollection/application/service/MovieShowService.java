package com.wikicollection.application.service;

import java.util.Objects;

import com.wikicollection.application.exception.MovieShowConflictException;
import com.wikicollection.application.exception.MovieShowNotFoundException;
import com.wikicollection.domain.model.MovieSearchCriteria;
import com.wikicollection.domain.model.MovieShow;
import com.wikicollection.domain.port.in.MovieShowUseCase;
import com.wikicollection.domain.port.out.MovieShowRepository;

import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MovieShowService implements MovieShowUseCase {

    private final MovieShowRepository movieShowRepository;
    private final DateRangeValidator dateRangeValidator;

    public MovieShowService(MovieShowRepository movieShowRepository,
                            DateRangeValidator dateRangeValidator) {
        this.movieShowRepository = movieShowRepository;
        this.dateRangeValidator = dateRangeValidator;
    }

    @Override
    public Page<MovieShow> search(MovieSearchCriteria criteria, Pageable pageable) {
        return movieShowRepository.findByCriteria(criteria, pageable);
    }

    @Override
    public MovieShow findById(String id) {
        return movieShowRepository.findById(id)
                .orElseThrow(() -> new MovieShowNotFoundException("Película/serie no encontrada con id: " + id));
    }

    @Override
    @Transactional
    public MovieShow save(MovieShow movieShow) {
        dateRangeValidator.validate(movieShow.getDateAdded(), movieShow.getDateCompleted());
        assertNoDuplicate(movieShow.getExternalId(), null);
        return saveOrConflict(movieShow);
    }

    @Override
    @Transactional
    public MovieShow update(String id, MovieShow updates) {
        MovieShow existing = findById(id);
        copyUpdatableFields(existing, updates);
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
    public void delete(String id) {
        findById(id);
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
    }
}
