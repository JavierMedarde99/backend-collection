package com.wikicollection.infrastructure.adapter.out.scryfall;

import java.util.List;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicLong;

import com.wikicollection.domain.model.MagicCard;
import com.wikicollection.domain.model.MagicCardPrinting;
import com.wikicollection.domain.model.MagicCardSearchResult;
import com.wikicollection.domain.port.out.ExternalMagicCardCatalogClient;
import com.wikicollection.infrastructure.adapter.out.scryfall.MagicCardMapper.ScryfallCardResponse;

import lombok.extern.slf4j.Slf4j;
import com.wikicollection.infrastructure.config.CacheConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.util.UriComponentsBuilder;

@Slf4j
@Component("scryfallClient")
public class ScryfallClient implements ExternalMagicCardCatalogClient {

    private static final String SEARCH_PATH = "/cards/search";
    private static final String NAMED_PATH = "/cards/named";
    private static final String CARD_PATH = "/cards";

    private final RestClient scryfallRestClient;
    private final String baseUrl;
    private final int retryAttempts;
    private final long retryDelayMs;
    private final long rateLimitDelayMs;
    private final MagicCardMapper mapper;
    private final AtomicLong lastRequestTime = new AtomicLong(0);

    @Autowired
    public ScryfallClient(@Qualifier("scryfallRestClient") RestClient scryfallRestClient,
                          @Value("${scryfall.api.base-url:https://api.scryfall.com}") String baseUrl,
                          @Value("${scryfall.api.retry-attempts:3}") int retryAttempts,
                          @Value("${scryfall.api.retry-delay-ms:1000}") long retryDelayMs,
                          @Value("${scryfall.api.rate-limit-delay-ms:100}") long rateLimitDelayMs,
                          MagicCardMapper mapper) {
        this.scryfallRestClient = scryfallRestClient;
        this.baseUrl = baseUrl;
        this.retryAttempts = retryAttempts;
        this.retryDelayMs = retryDelayMs;
        this.rateLimitDelayMs = rateLimitDelayMs;
        this.mapper = mapper;
    }

    public ScryfallClient(RestClient scryfallRestClient,
                          String baseUrl,
                          int retryAttempts,
                          long retryDelayMs,
                          MagicCardMapper mapper) {
        this(scryfallRestClient, baseUrl, retryAttempts, retryDelayMs, 0, mapper);
    }

    @Override
    @Cacheable(cacheNames = CacheConfig.MAGIC_SEARCH, key = "#query")
    public List<MagicCardSearchResult> search(String query) {
        if (query == null || query.isBlank()) {
            return List.of();
        }
        pace();
        String uri = UriComponentsBuilder.fromUriString(baseUrl)
                .path(SEARCH_PATH)
                .queryParam("q", query)
                .build()
                .toUriString();
        try {
            MagicCardMapper.ScryfallListResponse response =
                    executeWithRetry(() -> scryfallRestClient.get().uri(uri).retrieve().body(MagicCardMapper.ScryfallListResponse.class));
            return mapper.mapResponse(response);
        } catch (RestClientResponseException e) {
            log.warn("Scryfall devolvió error {}: {}", e.getStatusCode(), e.getMessage());
            return List.of();
        } catch (ResourceAccessException e) {
            log.warn("Scryfall no disponible: {}", e.getMessage());
            return List.of();
        }
    }

    @Override
    @Cacheable(cacheNames = CacheConfig.COMMANDER_SEARCH, key = "#colors")
    public List<MagicCardSearchResult> searchCommanders(String colors) {
        StringBuilder query = new StringBuilder("is:commander");
        if (colors != null && !colors.isBlank()) {
            query.append(" id<=").append(colors.trim().toLowerCase(Locale.ROOT));
        }
        pace();
        String uri = UriComponentsBuilder.fromUriString(baseUrl)
                .path(SEARCH_PATH)
                .queryParam("q", query.toString())
                .build()
                .toUriString();
        try {
            MagicCardMapper.ScryfallListResponse response =
                    executeWithRetry(() -> scryfallRestClient.get().uri(uri).retrieve().body(MagicCardMapper.ScryfallListResponse.class));
            return mapper.mapResponse(response);
        } catch (RestClientResponseException e) {
            log.warn("Scryfall devolvió error {}: {}", e.getStatusCode(), e.getMessage());
            return List.of();
        } catch (ResourceAccessException e) {
            log.warn("Scryfall no disponible: {}", e.getMessage());
            return List.of();
        }
    }

    @Override
    public MagicCard findById(String id) {
        pace();
        String uri = UriComponentsBuilder.fromUriString(baseUrl)
                .path(CARD_PATH)
                .pathSegment(id)
                .build()
                .toUriString();
        try {
            ScryfallCardResponse response =
                    executeWithRetry(() -> scryfallRestClient.get().uri(uri).retrieve().body(ScryfallCardResponse.class));
            return mapper.map(response);
        } catch (RestClientResponseException e) {
            log.warn("Scryfall devolvió error {} al obtener carta {}: {}", e.getStatusCode(), id, e.getMessage());
            throw e;
        } catch (ResourceAccessException e) {
            log.warn("Scryfall no disponible al obtener carta {}: {}", id, e.getMessage());
            throw e;
        }
    }

    @Override
    public MagicCard findByName(String name) {
        pace();
        String uri = UriComponentsBuilder.fromUriString(baseUrl)
                .path(NAMED_PATH)
                .queryParam("fuzzy", name)
                .build()
                .toUriString();
        try {
            ScryfallCardResponse response =
                    executeWithRetry(() -> scryfallRestClient.get().uri(uri).retrieve().body(ScryfallCardResponse.class));
            return mapper.map(response);
        } catch (RestClientResponseException e) {
            log.warn("Scryfall devolvió error {} al buscar carta {}: {}", e.getStatusCode(), name, e.getMessage());
            throw e;
        } catch (ResourceAccessException e) {
            log.warn("Scryfall no disponible al buscar carta {}: {}", name, e.getMessage());
            throw e;
        }
    }

    /**
     * Todas las impresiones de una carta. Scryfall no tiene endpoint de impresiones; la
     * búsqueda por {@code oracleid} con {@code unique=prints} sí, y es lo que devuelve una
     * fila por reimpresión física (una carta puede tener cientos).
     *
     * <p>A diferencia de {@link #search(String)} y {@link #findById(String)}, un fallo de
     * Scryfall se propaga en lugar de convertirse en una lista vacía: aquí el vacío es un
     * resultado con significado ("esta carta no tiene reimpresiones") y un 500 disfrazado
     * de vacío haría creer al usuario que su carta no se reimprimió.
     */
    @Override
    @Cacheable(cacheNames = CacheConfig.MAGIC_PRINTINGS, key = "#oracleId + ':' + #page",
            unless = "#result.content.isEmpty()")
    public Page<MagicCardPrinting> findPrintings(String oracleId, int page) {
        if (oracleId == null || oracleId.isBlank()) {
            // "q=oracleid:" sin valor devuelve 200 con 0 resultados, indistinguible de un
            // catálogo vacío. Es un fallo de programación, no una consulta vacía.
            return new PageImpl<>(java.util.List.of(),
                    PageRequest.of(Math.max(page, 0), MagicCardPrinting.PAGE_SIZE), 0);
        }
        pace();
        String uri = UriComponentsBuilder.fromUriString(baseUrl)
                .path(SEARCH_PATH)
                .queryParam("q", "oracleid:" + oracleId)
                .queryParam("unique", "prints")
                .queryParam("order", "released")
                // Scryfall pagina en base 1; la de nuestra API es base 0.
                .queryParam("page", Math.max(page, 0) + 1)
                .build()
                .toUriString();
        MagicCardMapper.ScryfallListResponse response =
                executeWithRetry(() -> scryfallRestClient.get().uri(uri).retrieve().body(MagicCardMapper.ScryfallListResponse.class));
        return mapper.mapPrintings(response, page);
    }

    private <T> T executeWithRetry(IoSupplier<T> supplier) {
        int attempts = retryAttempts + 1;
        RestClientResponseException last = null;
        for (int i = 0; i < attempts; i++) {
            try {
                return supplier.get();
            } catch (RestClientResponseException e) {
                last = e;
                if (i < attempts - 1) {
                    log.info("Scryfall devolvió {}, reintentando ({}/{})", e.getStatusCode(), i + 1, attempts);
                    sleep();
                }
            } catch (ResourceAccessException e) {
                last = null;
                if (i < attempts - 1) {
                    log.info("Scryfall no respondió, reintentando ({}/{})", i + 1, attempts);
                    sleep();
                } else {
                    throw e;
                }
            }
        }
        throw last;
    }

    private void pace() {
        if (rateLimitDelayMs <= 0) {
            return;
        }
        synchronized (this) {
            long waitMs = lastRequestTime.get() + rateLimitDelayMs - System.currentTimeMillis();
            if (waitMs > 0) {
                sleepMs(waitMs);
            }
            lastRequestTime.set(System.currentTimeMillis());
        }
    }

    private void sleep() {
        sleepMs(retryDelayMs);
    }

    private void sleepMs(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    @FunctionalInterface
    private interface IoSupplier<T> {
        T get();
    }
}
