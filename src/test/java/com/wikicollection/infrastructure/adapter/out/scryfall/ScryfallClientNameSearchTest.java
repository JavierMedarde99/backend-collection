package com.wikicollection.infrastructure.adapter.out.scryfall;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

import com.wikicollection.domain.model.MagicCardSearchResult;

import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

/**
 * Búsquedas por nombre que usa la importación de mazos (#353).
 *
 * <p>Dos propiedades que importan y que no se pueden intuir leyendo el cliente:
 * {@code unique=oracle} es obligatorio (sin él Scryfall devuelve las veinte reimpresiones de
 * "Sol Ring" y cada línea del mazo parecería ambigua), y un fallo de Scryfall **se propaga**
 * en vez de convertirse en una lista vacía, porque aquí el vacío significa "esta carta no
 * existe" y un 500 disfrazado de vacío haría que la importación guardara el mazo sin esa
 * carta y le dijera al usuario que no existe.
 */
class ScryfallClientNameSearchTest {

    private MockWebServer server;
    private ScryfallClient client;

    @BeforeEach
    void setUp() throws Exception {
        server = new MockWebServer();
        server.start();
        client = new ScryfallClient(
                RestClient.builder().baseUrl(server.url("").toString()).build(),
                server.url("").toString(), 0, 0, new MagicCardMapper());
    }

    @AfterEach
    void tearDown() throws Exception {
        server.shutdown();
    }

    @Test
    void searchByNameExact_sendsNameQueryWithUniqueOracle() throws Exception {
        server.enqueue(listResponse(1));

        List<MagicCardSearchResult> results = client.searchByNameExact("Sol Ring");

        assertThat(results).hasSize(1);
        assertThat(results.get(0).name()).isEqualTo("Carta 1");
        assertThat(results.get(0).scryfallId()).isEqualTo("id-1");

        String query = decodedQuery(server.takeRequest());
        assertThat(query).contains("q=name:\"Sol Ring\"");
        assertThat(query).contains("unique=oracle");
    }

    @Test
    void searchByNameExact_blankName_returnsEmptyWithoutCallingScryfall() {
        assertThat(client.searchByNameExact("   ")).isEmpty();
        assertThat(client.searchByNameExact(null)).isEmpty();
        assertThat(server.getRequestCount()).isZero();
    }

    @Test
    void searchSuggestions_sendsBareQueryAndCapsAtFive() throws Exception {
        server.enqueue(listResponse(8));

        List<MagicCardSearchResult> results = client.searchSuggestions("Sol Ring");

        assertThat(results).hasSize(5);
        assertThat(results.get(4).name()).isEqualTo("Carta 5");

        String query = decodedQuery(server.takeRequest());
        assertThat(query).contains("q=Sol Ring");
        assertThat(query).contains("unique=oracle");
        // Scryfall ignora page_size/per_page/limit en silencio, así que no se mandan: el
        // recorte a cinco es nuestro, y un parámetro mal escrito devuelve 200 con 0
        // resultados en lugar de un error.
        assertThat(query).doesNotContain("name:\"");
        assertThat(query).doesNotContain("page_size");
        assertThat(query).doesNotContain("per_page");
        assertThat(query).doesNotContain("limit=");
    }

    @Test
    void searchSuggestions_blankName_returnsEmptyWithoutCallingScryfall() {
        assertThat(client.searchSuggestions(" ")).isEmpty();
        assertThat(client.searchSuggestions(null)).isEmpty();
        assertThat(server.getRequestCount()).isZero();
    }

    @Test
    void searchByNameExact_propagatesUpstreamError() {
        for (int i = 0; i < 4; i++) {
            server.enqueue(new MockResponse().setResponseCode(503));
        }
        ScryfallClient retrying = new ScryfallClient(
                RestClient.builder().baseUrl(server.url("").toString()).build(),
                server.url("").toString(), 3, 0, new MagicCardMapper());

        assertThatThrownBy(() -> retrying.searchByNameExact("Sol Ring"))
                .isInstanceOf(RestClientResponseException.class);
        assertThat(server.getRequestCount()).isEqualTo(4);
    }

    @Test
    void searchSuggestions_propagatesUpstreamError() {
        server.enqueue(new MockResponse().setResponseCode(503));

        assertThatThrownBy(() -> client.searchSuggestions("Sol Ring"))
                .isInstanceOf(RestClientResponseException.class);
    }

    @Test
    void searchByNameExact_returnsEmpty_whenScryfallHasNoMatch() throws Exception {
        server.enqueue(listResponse(0));

        assertThat(client.searchByNameExact("Llanuras")).isEmpty();
    }

    private String decodedQuery(RecordedRequest request) {
        String path = request.getPath();
        int mark = path.indexOf('?');
        String query = mark < 0 ? "" : path.substring(mark + 1);
        return URLDecoder.decode(query.replace("+", "%20"), StandardCharsets.UTF_8);
    }

    private MockResponse listResponse(int count) {
        StringBuilder data = new StringBuilder();
        for (int i = 1; i <= count; i++) {
            if (i > 1) {
                data.append(',');
            }
            data.append("""
                    {
                      "id": "id-%d",
                      "oracle_id": "oracle-%d",
                      "name": "Carta %d",
                      "type_line": "Instant",
                      "colors": [],
                      "color_identity": [],
                      "rarity": "common",
                      "set": "tst",
                      "set_name": "Test Set",
                      "layout": "normal"
                    }""".formatted(i, i, i));
        }
        return new MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                .setBody("{\"object\":\"list\",\"total_cards\":" + count + ",\"data\":[" + data + "]}");
    }
}