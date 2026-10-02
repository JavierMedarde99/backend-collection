package com.wikicollection.infrastructure.adapter.out.scryfall;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.wikicollection.domain.model.MagicCardPrinting;

import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

/**
 * Las aserciones sobre la URL son el contrato con Scryfall, no un detalle: el servicio
 * ignora en silencio los parametros mal escritos, asi que un {@code direction=} en lugar
 * de {@code dir=} devolveria 200 con 0 resultados y el fallo apareceria como "el endpoint
 * no encuentra nada".
 */
class ScryfallClientPrintingsTest {

    private MockWebServer server;
    private ScryfallClient client;

    @BeforeEach
    void setUp() throws Exception {
        server = new MockWebServer();
        server.start();
        // Mismo requestFactory que usa el bean scryfallRestClient en produccion: sin el,
        // el test pasa y el bug solo aparece en el contexto de Spring.
        client = new ScryfallClient(
                RestClient.builder().baseUrl(server.url("").toString())
                        .requestFactory(new org.springframework.http.client.SimpleClientHttpRequestFactory())
                        .build(),
                server.url("").toString(), 0, 0, new MagicCardMapper());
    }

    @AfterEach
    void tearDown() throws Exception {
        server.shutdown();
    }

    private void enqueuePrintings(String totalCards, int returned) {
        StringBuilder data = new StringBuilder();
        for (int i = 0; i < returned; i++) {
            if (i > 0) {
                data.append(',');
            }
            data.append("{\"object\":\"card\",\"id\":\"id-").append(i)
                    .append("\",\"oracle_id\":\"oracle-x\",\"name\":\"Llanuras\",\"set\":\"dce\"}");
        }
        server.enqueue(new MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                .setBody("{\"object\":\"list\",\"total_cards\":" + totalCards + ",\"data\":[" + data + "]}"));
    }

    @Test
    void paginaPorOracleIdOrdenadaPorLanzamientoYConUniquePrints() throws Exception {
        enqueuePrintings("955", 2);

        var page = client.findPrintings("oracle-x", 0);

        assertThat(page.getContent()).extracting(MagicCardPrinting::scryfallId)
                .containsExactly("id-0", "id-1");
        assertThat(page.getTotalElements()).isEqualTo(955);

        RecordedRequest request = server.takeRequest();
        String path = request.getPath();
        assertThat(request.getMethod()).isEqualTo("GET");
        assertThat(path).startsWith("/cards/search?");
        assertThat(path).contains("q=oracleid:oracle-x");
        assertThat(path).contains("unique=prints");
        assertThat(path).contains("order=released");
    }

    @Test
    void laPaginaSePideEnBaseUnoAunqueLaApiSeaBaseCero() throws Exception {
        enqueuePrintings("1000", 1);
        client.findPrintings("oracle-x", 0);
        assertThat(server.takeRequest().getPath()).contains("page=1");

        enqueuePrintings("1000", 1);
        client.findPrintings("oracle-x", 3);
        assertThat(server.takeRequest().getPath()).contains("page=4");
    }

    @Test
    void laPaginaDevueltaArrancaEnElIndicePedido() throws Exception {
        enqueuePrintings("955", 1);

        var page = client.findPrintings("oracle-x", 3);

        assertThat(page.getNumber()).isEqualTo(3);
    }

    @Test
    void noPidePageSizePorqueScryfallLoIgnora() throws Exception {
        enqueuePrintings("955", 1);

        client.findPrintings("oracle-x", 0);

        assertThat(server.takeRequest().getPath()).doesNotContain("page_size");
    }

    @Test
    void usaLaBusquedaPorOracleIdYNoUnaRutaDeImpresionesInexistente() throws Exception {
        enqueuePrintings("10", 1);

        client.findPrintings("oracle-x", 0);

        // /cards/{id}/prints no existe en Scryfall; la busqueda por oracleid es la que funciona.
        assertThat(server.takeRequest().getPath()).startsWith("/cards/search");
    }

    @Test
    void unaPaginaVaciaNoEsUnError() throws Exception {
        enqueuePrintings("5", 0);

        var page = client.findPrintings("oracle-x", 9);

        assertThat(page.getContent()).isEmpty();
        assertThat(page.getTotalElements()).isEqualTo(5);
    }

    @Test
    void propagaElErrorDeScryfallSinTragarselo() {
        server.enqueue(new MockResponse().setResponseCode(500));

        // A diferencia de search(), aqui un fallo no puede disfrazarse de "no hay
        // impresiones": el usuario veria una lista vacia y creeria que la carta nunca se
        // reimprimio.
        assertThatThrownBy(() -> client.findPrintings("oracle-x", 0))
                .isInstanceOf(RestClientResponseException.class)
                .satisfies(e -> assertThat(((RestClientResponseException) e).getStatusCode())
                        .isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR));
    }

    @Test
    void unOracleIdVacioNoLlamaAScryfall() {
        // "q=oracleid:" sin valor devuelve 200 con 0 resultados, indistinguible de un
        // catalogo vacio.
        assertThat(client.findPrintings(null, 0).getContent()).isEmpty();
        assertThat(client.findPrintings("  ", 0).getContent()).isEmpty();
        assertThat(server.getRequestCount()).isZero();
    }

    @Test
    void elOracleIdSeEscapaEnLaQuery() throws Exception {
        enqueuePrintings("1", 1);

        client.findPrintings("oracle with spaces", 0);

        assertThat(server.takeRequest().getPath()).contains("q=oracleid:oracle%20with%20spaces");
    }

    @Test
    void reintentaAnteUnFalloDeServidor() throws Exception {
        // El cliente de test se construye con 0 reintentos; aquí hace falta uno.
        ScryfallClient retrying = new ScryfallClient(
                RestClient.builder().baseUrl(server.url("").toString()).build(),
                server.url("").toString(), 1, 0, new MagicCardMapper());
        server.enqueue(new MockResponse().setResponseCode(503));
        enqueuePrintings("2", 1);

        var page = retrying.findPrintings("oracle-x", 0);

        assertThat(page.getContent()).hasSize(1);
        assertThat(server.getRequestCount()).isEqualTo(2);
    }

    @Test
    void noVuelveAPedirLaCartaParaResolverElOracleId() throws Exception {
        enqueuePrintings("1", 1);

        client.findPrintings("oracle-x", 0);

        // El cliente recibe el oracle_id ya resuelto: una sola peticion.
        assertThat(server.getRequestCount()).isEqualTo(1);
        assertThat(server.takeRequest().getPath()).doesNotContain("/cards/oracle-x");
    }

    @Test
    void cadaImpresionTraeSuPropioScryfallId() throws Exception {
        enqueuePrintings("2", 2);

        var page = client.findPrintings("oracle-x", 0);

        // Es el valor que se pasa a POST /magic/scryfall/{id}: si se repitiera el id de la
        // carta, el usuario volveria a guardar siempre la misma impresion.
        assertThat(page.getContent()).extracting(MagicCardPrinting::scryfallId)
                .containsExactly("id-0", "id-1")
                .doesNotHaveDuplicates();
    }
}
