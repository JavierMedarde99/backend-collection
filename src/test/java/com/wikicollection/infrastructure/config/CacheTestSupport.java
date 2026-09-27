package com.wikicollection.infrastructure.config;

import org.springframework.cache.CacheManager;

/**
 * Test helper for the Caffeine caches declared in {@link CacheConfig}.
 *
 * <p>Spring reuses a single application context (and therefore a single {@link CacheManager})
 * across every test class that shares the same configuration. Any {@code @Cacheable} service
 * method therefore keeps entries between test methods, so a test that stubs a repository with
 * the same id (for example {@code "b1"}) can be served a previously cached instance instead of
 * its own stub. Clearing every cache before each test method keeps those tests isolated.
 */
public final class CacheTestSupport {

    private CacheTestSupport() {
    }

    public static void clearAll(CacheManager cacheManager) {
        for (String name : CacheConfig.CACHE_NAMES) {
            var cache = cacheManager.getCache(name);
            if (cache != null) {
                cache.clear();
            }
        }
    }
}
