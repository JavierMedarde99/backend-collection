package com.wikicollection.application.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import com.wikicollection.domain.model.MagicCardPrinting;
import com.wikicollection.domain.port.in.MagicCardUseCase;
import com.wikicollection.infrastructure.config.CacheConfig;

import okhttp3.mockwebserver.Dispatcher;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

/**
 * El criterio "respuesta cacheada en magicPrintings" importa por una razón concreta: si
 * el nombre no estuviera registrado en CacheConfig, CaffeineCacheManager crearía la caché
 * sin TTL ni tope de tamaño y nada fallaría — solo crecería para siempre. Estos tests
 * fijan que la segunda llamada idéntica no vuelve a pegarle a Scryfall.
 *
 * <p>Usa un Dispatcher en lugar de una cola enrutada por la URL: la cola de MockWebServer
 * sobrevive entre tests de la misma clase y las respuestas sobrantes se consumen en el
 * test siguiente.
 */
@SpringBootTest(properties = {
        "spring.data.mongodb.auto-index-creation=false",
        "app.boardgame-status-migration.enabled=false"})
class MagicCardPrintingsCacheTest {

    private static MockWebServer server;
    private static final Map<String, String> ORACLE_BY_CARD_ID = new ConcurrentHashMap<>();
    private static final AtomicInteger SEARCH_CALLS = new AtomicInteger();
    private static final AtomicLong SEARCH_TOTAL = new AtomicLong(955);
    private static final AtomicInteger SEARCH_RESULT_COUNT = new AtomicInteger(2);

    @Autowired
    private MagicCardUseCase magicCardUseCase;

    @Autowired
    private CacheManager cacheManager;

    @BeforeAll
    static void startServer() throws Exception {
        server = new MockWebServer();
        server.setDispatcher(new Dispatcher() {
            @Override
            public MockResponse dispatch(RecordedRequest request) {
                String path = String.valueOf(request.getPath());
                if (path.startsWith("/cards/search")) {
                    SEARCH_CALLS.incrementAndGet();
                    return json(listResponse());
                }
                String cardId = path.substring(path.lastIndexOf('/') + 1);
                return json(cardResponse(ORACLE_BY_CARD_ID.getOrDefault(cardId, "oracle-x")));
            }
        });
        server.start();
    }

    @AfterAll
    static void stopServer() throws Exception {
        server.shutdown();
    }

    @DynamicPropertySource
    static void scryfallBaseUrl(DynamicPropertyRegistry registry) {
        registry.add("scryfall.api.base-url", () -> server.url("").toString());
        registry.add("scryfall.api.retry-attempts", () -> 0);
        registry.add("scryfall.api.retry-delay-ms", () -> 0);
        registry.add("scryfall.api.rate-limit-delay-ms", () -> 0);
    }

    @BeforeEach
    void reset() {
        ORACLE_BY_CARD_ID.clear();
        SEARCH_CALLS.set(0);
        SEARCH_TOTAL.set(955);
        SEARCH_RESULT_COUNT.set(2);
        Cache cache = cacheManager.getCache(CacheConfig.MAGIC_PRINTINGS);
        if (cache != null) {
            cache.clear();
        }
    }

    private static MockResponse json(String body) {
        return new MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                .setBody(body);
    }

    private static String cardResponse(String oracleId) {
        return "{\"object\":\"card\",\"id\":\"print-1\",\"oracle_id\":\"" + oracleId
                + "\",\"name\":\"Llanuras\",\"set\":\"dce\"}";
    }

    private static String listResponse() {
        StringBuilder data = new StringBuilder();
        for (int i = 0; i < SEARCH_RESULT_COUNT.get(); i++) {
            if (i > 0) {
                data.append(',');
            }
            data.append("{\"object\":\"card\",\"id\":\"print-").append(i)
                    .append("\",\"oracle_id\":\"oracle-x\",\"name\":\"Llanuras\",\"set\":\"dce\"}");
        }
        return "{\"object\":\"list\",\"total_cards\":" + SEARCH_TOTAL.get() + ",\"data\":[" + data + "]}";
    }

    @Test
    void laSegundaPeticionIgualNoVuelveABuscarImpresiones() {
        ORACLE_BY_CARD_ID.put("print-1", "oracle-x");

        var first = magicCardUseCase.printings("print-1", 0);
        var second = magicCardUseCase.printings("print-1", 0);

        assertThat(second.getContent()).hasSameSizeAs(first.getContent());
        assertThat(second.getTotalElements()).isEqualTo(first.getTotalElements());
        assertThat(SEARCH_CALLS.get()).isEqualTo(1);
    }

    @Test
    void paginasDistintasSonEntradasDistintas() {
        ORACLE_BY_CARD_ID.put("print-1", "oracle-x");

        var first = magicCardUseCase.printings("print-1", 0);
        SEARCH_TOTAL.set(400);
        var second = magicCardUseCase.printings("print-1", 1);

        // Si la clave no incluyera el número de página, la segunda respuesta sería la
        // primera y el usuario no podría avanzar de página.
        assertThat(SEARCH_CALLS.get()).isEqualTo(2);
        assertThat(second.getNumber()).isEqualTo(1);
        assertThat(second.getTotalElements()).isNotEqualTo(first.getTotalElements());
    }

    @Test
    void cartasDistintasNoCompartenCache() {
        ORACLE_BY_CARD_ID.put("print-a", "oracle-a");
        ORACLE_BY_CARD_ID.put("print-b", "oracle-b");

        // Totales por encima de 175: PageImpl recorta los menores al tamaño de la pagina.
        SEARCH_TOTAL.set(400);
        var a = magicCardUseCase.printings("print-a", 0);
        SEARCH_TOTAL.set(700);
        var b = magicCardUseCase.printings("print-b", 0);

        assertThat(a.getTotalElements()).isEqualTo(400);
        assertThat(b.getTotalElements()).isEqualTo(700);
    }

    @Test
    void unaPaginaVaciaNoSeCachea() {
        ORACLE_BY_CARD_ID.put("print-1", "oracle-x");
        SEARCH_RESULT_COUNT.set(0);
        SEARCH_TOTAL.set(5);

        magicCardUseCase.printings("print-1", 0);
        magicCardUseCase.printings("print-1", 0);

        // Cachear un vacío fijaría "esta carta no tiene reimpresiones" aunque Scryfall
        // estuviera caído al empezar.
        assertThat(SEARCH_CALLS.get()).isEqualTo(2);
    }

    @Test
    void unaPaginaConImpresionesSiSeCachea() {
        ORACLE_BY_CARD_ID.put("print-1", "oracle-x");

        magicCardUseCase.printings("print-1", 0);
        magicCardUseCase.printings("print-1", 0);

        assertThat(SEARCH_CALLS.get()).isEqualTo(1);
    }

    @Test
    void laCacheMagicPrintingsExisteYEstaRegistrada() {
        Cache cache = cacheManager.getCache(CacheConfig.MAGIC_PRINTINGS);

        assertThat(cache).isNotNull();
        assertThat(cache.getName()).isEqualTo(CacheConfig.MAGIC_PRINTINGS);
    }

    @Test
    void laRespuestaTraeElTotalRealDeScryfall() {
        ORACLE_BY_CARD_ID.put("print-1", "oracle-x");
        SEARCH_TOTAL.set(955);

        var page = magicCardUseCase.printings("print-1", 0);

        assertThat(page.getContent()).hasSize(2);
        assertThat(page.getTotalElements()).isEqualTo(955);
        assertThat(page.getTotalPages()).isEqualTo(6);
        assertThat(page.getSize()).isEqualTo(MagicCardPrinting.PAGE_SIZE);
    }
}
