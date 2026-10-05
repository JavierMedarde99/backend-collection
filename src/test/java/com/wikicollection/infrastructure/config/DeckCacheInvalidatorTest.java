package com.wikicollection.infrastructure.config;

import static org.assertj.core.api.Assertions.assertThat;

import com.github.benmanes.caffeine.cache.Caffeine;
import com.wikicollection.application.service.DeckCacheInvalidator;
import com.wikicollection.application.service.OwnerResolver;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.cache.interceptor.SimpleKey;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

/**
 * La evictación de las cachés de mazos después de importar (#353).
 *
 * <p>Lo que se comprueba aquí es que {@code afterImport()} funcione <strong>a través del
 * proxy</strong>, igual que lo llama el worker desde otro bean. Si se llamara desde dentro
 * del propio bean, el advisor de caché no se ejecutaría y la invalidación sería
 * silenciosamente inútil: el usuario seguiría viendo los mazos anteriores a la importación
 * sin que nada fallara ni avisara.
 */
@SpringBootTest(classes = DeckCacheInvalidatorTest.TestConfig.class)
class DeckCacheInvalidatorTest {

    @Autowired
    private DeckCacheInvalidator invalidator;

    @Autowired
    private CacheManager cacheManager;

    @MockitoBean
    private OwnerResolver ownerResolver;

    @Test
    void afterImport_viaProxy_clearsTheDeckCaches() {
        Cache deckDetail = cacheManager.getCache(CacheConfig.DECK_DETAIL);
        Cache deckList = cacheManager.getCache(CacheConfig.DECK_LIST);
        deckDetail.put(new SimpleKey("deck-1"), "mazo viejo");
        deckList.put(new SimpleKey(0), "lista vieja");
        assertThat(deckDetail.get(new SimpleKey("deck-1"))).isNotNull();
        assertThat(deckList.get(new SimpleKey(0))).isNotNull();

        invalidator.afterImport();

        assertThat(deckDetail.get(new SimpleKey("deck-1"))).isNull();
        assertThat(deckList.get(new SimpleKey(0))).isNull();
    }

    @Configuration
    @EnableCaching
    static class TestConfig {

        @Bean
        DeckCacheInvalidator deckCacheInvalidator() {
            return new DeckCacheInvalidator();
        }

        @Bean
        CacheManager cacheManager() {
            CaffeineCacheManager manager = new CaffeineCacheManager(CacheConfig.DECK_DETAIL, CacheConfig.DECK_LIST);
            manager.setCaffeine(Caffeine.newBuilder());
            return manager;
        }
    }
}