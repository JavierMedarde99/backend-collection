package com.wikicollection.infrastructure.config;

import java.time.Duration;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.wikicollection.domain.model.DeckImportJob;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

/**
 * Cableado de la importación asíncrona de mazos (#353).
 *
 * <p>El executor va con nombre porque el {@code @Async} lo referencia por él. Importa que la
 * cola sea pequeña: si se llena, la importación se rechaza con 429 en lugar de dejar que las
 * peticiones se acumulen sin límite.
 */
@Configuration
@EnableAsync
public class AsyncConfig {

    @Bean("deckImportExecutor")
    public ThreadPoolTaskExecutor deckImportExecutor(
            @Value("${deck.import.executor.core-size:2}") int coreSize,
            @Value("${deck.import.executor.max-size:4}") int maxSize,
            @Value("${deck.import.executor.queue-capacity:20}") int queueCapacity) {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(coreSize);
        executor.setMaxPoolSize(maxSize);
        executor.setQueueCapacity(queueCapacity);
        executor.setThreadNamePrefix("deck-import-");
        executor.initialize();
        return executor;
    }

    /**
     * Registro de trabajos, aparte del {@code CacheManager}: este se consulta directamente y no
     * comparte TTL ni eviction con las cachés de lectura.
     */
    @Bean("deckImportJobs")
    public Cache<String, DeckImportJob> deckImportJobs(
            @Value("${deck.import.job.ttl:30m}") Duration ttl,
            @Value("${deck.import.job.max-size:200}") long maxSize) {
        return Caffeine.newBuilder()
                .expireAfterWrite(ttl)
                .maximumSize(maxSize)
                .build();
    }
}