package com.wikicollection.infrastructure.config;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.Map;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.convert.DurationUnit;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "app.cache")
public class CacheProperties {

    @DurationUnit(ChronoUnit.SECONDS)
    private Duration defaultTtl = Duration.ofHours(1);

    private long maxSize = 1000;

    @DurationUnit(ChronoUnit.SECONDS)
    private Map<String, Duration> ttl = new HashMap<>();

    public Duration getDefaultTtl() {
        return defaultTtl;
    }

    public void setDefaultTtl(Duration defaultTtl) {
        this.defaultTtl = defaultTtl;
    }

    public long getMaxSize() {
        return maxSize;
    }

    public void setMaxSize(long maxSize) {
        this.maxSize = maxSize;
    }

    public Map<String, Duration> getTtl() {
        return ttl;
    }

    public void setTtl(Map<String, Duration> ttl) {
        this.ttl = ttl;
    }

    public Duration ttlFor(String cacheName) {
        return ttl.getOrDefault(cacheName, defaultTtl);
    }
}
