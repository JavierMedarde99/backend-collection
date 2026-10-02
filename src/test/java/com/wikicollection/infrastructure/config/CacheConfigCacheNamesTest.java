package com.wikicollection.infrastructure.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

/**
 * Un nombre de caché registrado en {@link CacheConfig#CACHE_NAMES} sin entrada en
 * {@code PROPERTY_KEYS} no falla: CaffeineCacheManager queda en modo dinámico y crea
 * la caché bajo demanda, pero sin TTL ni tope de tamaño. Estos guards lo detectan.
 */
class CacheConfigCacheNamesTest {

    @Test
    void everyRegisteredCacheNameHasAPropertyKey() throws Exception {
        Map<String, String> propertyKeys = propertyKeys();

        assertThat(CacheConfig.CACHE_NAMES)
                .allSatisfy(name -> assertThat(propertyKeys)
                        .as("cache sin clave de TTL: %s", name)
                        .containsKey(name));
    }

    @Test
    void everyPropertyKeyPointsToARegisteredCacheName() throws Exception {
        assertThat(propertyKeys().keySet()).isSubsetOf(CacheConfig.CACHE_NAMES);
    }

    @Test
    void propertyKeysAndCacheNamesDoNotDriftInSize() throws Exception {
        assertThat(propertyKeys()).hasSameSizeAs(CacheConfig.CACHE_NAMES);
    }

    @Test
    void platformSearchIsRegistered_soItGetsATtlAndMaxSize() {
        assertThat(CacheConfig.CACHE_NAMES).contains(CacheConfig.PLATFORM_SEARCH);
    }

    @Test
    void magicPrintingsIsRegistered_soItGetsATtlAndMaxSize() {
        assertThat(CacheConfig.CACHE_NAMES).contains(CacheConfig.MAGIC_PRINTINGS);
    }

    @SuppressWarnings("unchecked")
    private Map<String, String> propertyKeys() throws Exception {
        Field field = CacheConfig.class.getDeclaredField("PROPERTY_KEYS");
        field.setAccessible(true);
        return new HashMap<>((Map<String, String>) field.get(null));
    }
}