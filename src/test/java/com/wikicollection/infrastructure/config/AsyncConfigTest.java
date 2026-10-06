package com.wikicollection.infrastructure.config;

import static org.assertj.core.api.Assertions.assertThat;

import com.github.benmanes.caffeine.cache.Cache;
import com.wikicollection.application.service.OwnerResolver;
import com.wikicollection.domain.model.DeckImportJob;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.CacheManager;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

/**
 * El cableado de la importación asíncrona (#353).
 *
 * <p>Dos cosas que no se pueden comprobar leyendo el código: que el executor tenga el tamaño
 * que dicen las propiedades, y que el registro de trabajos no se cuele en el
 * {@code CacheManager} de Spring Cache (que solo conoce los nombres de
 * {@link CacheConfig#CACHE_NAMES} y aplica su propia TTL).
 */
@SpringBootTest(properties = {
        "spring.data.mongodb.auto-index-creation=false",
        "app.boardgame-status-migration.enabled=false"})
class AsyncConfigTest {

    @MockitoBean
    private OwnerResolver ownerResolver;

    @Autowired
    @Qualifier("deckImportExecutor")
    private ThreadPoolTaskExecutor deckImportExecutor;

    @Autowired
    @Qualifier("deckImportJobs")
    private Cache<String, DeckImportJob> deckImportJobs;

    @Autowired
    private CacheManager cacheManager;

    @Test
    void executorBeanExistsWithExpectedPool() {
        assertThat(deckImportExecutor.getCorePoolSize()).isEqualTo(2);
        assertThat(deckImportExecutor.getMaxPoolSize()).isEqualTo(4);
        assertThat(deckImportExecutor.getThreadPoolExecutor().getQueue().remainingCapacity()).isPositive();
    }

    @Test
    void deckImportJobsIsItsOwnCaffeineCacheNotSpringManaged() {
        assertThat(deckImportJobs).isNotNull();

        assertThat(CacheConfig.CACHE_NAMES).doesNotContain("deckImportJobs");
        assertThat(cacheManager.getCache("deckImportJobs")).isNull();
    }

    @Test
    void deckImportJobsHoldsWhatTheStorePutsInIt() {
        assertThat(deckImportJobs.getIfPresent("nope")).isNull();
    }
}