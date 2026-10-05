package com.wikicollection.application.service;

import java.util.Optional;

import com.github.benmanes.caffeine.cache.Cache;
import com.wikicollection.domain.model.DeckImportJob;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

/**
 * Registro en memoria de los trabajos de importación (#353).
 *
 * <p>Deliberadamente **no** es un {@code CacheManager} de Spring Cache: ahí solo se guardan
 * los nombres de {@code CacheConfig.CACHE_NAMES} y la TTL la gobierna la configuración. Este
 * registro lo consulta el store directamente y caduca entero, porque un job se mira una vez
 * mientras corre y no tiene sentido cachearlo.
 */
@Component
public class DeckImportJobStore {

    private final Cache<String, DeckImportJob> cache;

    public DeckImportJobStore(@Qualifier("deckImportJobs") Cache<String, DeckImportJob> cache) {
        this.cache = cache;
    }

    public void save(DeckImportJob job) {
        cache.put(job.jobId(), job);
    }

    public Optional<DeckImportJob> find(String jobId) {
        return jobId == null ? Optional.empty() : Optional.ofNullable(cache.getIfPresent(jobId));
    }

    public void evict(String jobId) {
        if (jobId != null) {
            cache.invalidate(jobId);
        }
    }
}