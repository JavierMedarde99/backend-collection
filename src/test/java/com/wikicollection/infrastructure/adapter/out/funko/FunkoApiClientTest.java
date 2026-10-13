package com.wikicollection.infrastructure.adapter.out.funko;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.client.ExpectedCount.once;
import static org.springframework.test.web.client.ExpectedCount.times;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.queryParam;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withException;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

import com.wikicollection.domain.model.FunkoCategory;
import com.wikicollection.domain.model.FunkoSearchResult;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

class FunkoApiClientTest {

    private static final String BASE_URL = "https://hobbyquanthq.com/api/v1";

    private MockRestServiceServer server;
    private FunkoApiClient client;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder().baseUrl(BASE_URL);
        server = MockRestServiceServer.bindTo(builder).build();
        client = new FunkoApiClient(builder.build(), "test-key", 0);
    }

    @Test
    void searchByName_sendsApiKeyHeader_andLimit100() {
        server.expect(once(), requestTo(containsString("/search")))
                .andExpect(header("x-api-key", "test-key"))
                .andExpect(queryParam("q", "batman"))
                .andExpect(queryParam("limit", "100"))
                .andRespond(withSuccess("{\"results\":[],\"query\":\"batman\",\"total\":0}",
                        MediaType.APPLICATION_JSON));

        assertThat(client.searchByName("batman")).isEmpty();
    }

    @Test
    void searchByName_mapsAllFields() {
        server.expect(once(), requestTo(containsString("/search")))
                .andRespond(withSuccess("""
                        {"results":[{"id":44,"name":"Batman","series":"DC Comics","number":"01","category":"Comics",
                        "exclusive":null,"image_url":"https://images.hobbydb.com/x.jpg","vaulted":true,
                        "release_year":2012,"franchise":null,"pop_type":null,"rarity_score":30,"image_valid":true}],
                        "query":"batman","total":1}
                        """, MediaType.APPLICATION_JSON));

        List<FunkoSearchResult> results = client.searchByName("batman");

        assertThat(results).hasSize(1);
        FunkoSearchResult r = results.get(0);
        assertThat(r.externalId()).isEqualTo("44");
        assertThat(r.name()).isEqualTo("Batman");
        assertThat(r.series()).isEqualTo("DC Comics");
        assertThat(r.number()).isEqualTo("01");
        assertThat(r.category()).isEqualTo("Comics");
        assertThat(r.exclusive()).isNull();
        assertThat(r.vaulted()).isTrue();
        assertThat(r.releaseYear()).isEqualTo(2012);
        assertThat(r.franchise()).isNull();
        assertThat(r.popType()).isNull();
        assertThat(r.rarityScore()).isEqualTo(30);
        assertThat(r.imageUrl()).contains("hobbydb");
        assertThat(r.externalSource()).isEqualTo("hobbyquanthq");
    }

    @Test
    void searchByName_returnsEmpty_whenResultsEmpty() {
        server.expect(once(), requestTo(containsString("/search")))
                .andRespond(withSuccess("{\"results\":[],\"query\":\"zzzz\",\"total\":0}",
                        MediaType.APPLICATION_JSON));

        assertThat(client.searchByName("zzzz")).isEmpty();
    }

    @Test
    void searchByName_returnsEmpty_whenNullContainer() {
        server.expect(once(), requestTo(containsString("/search")))
                .andRespond(withSuccess("{\"results\":null}", MediaType.APPLICATION_JSON));

        assertThat(client.searchByName("batman")).isEmpty();
    }

    @Test
    void searchByName_retries5xx_4Attempts_thenRethrows() {
        server.expect(times(4), requestTo(containsString("/search")))
                .andRespond(withServerError());

        assertThatThrownBy(() -> client.searchByName("batman"))
                .isInstanceOf(RestClientResponseException.class);
        server.verify();
    }

    @Test
    void searchByName_propagatesTimeout() {
        server.expect(times(4), requestTo(containsString("/search")))
                .andRespond(withException(new IOException("read timeout")));

        assertThatThrownBy(() -> client.searchByName("batman"))
                .isInstanceOf(ResourceAccessException.class);
        server.verify();
    }

    @Test
    void searchByName_doesNotRetry4xx() {
        server.expect(once(), requestTo(containsString("/search")))
                .andRespond(withStatus(HttpStatus.UNPROCESSABLE_ENTITY));

        assertThatThrownBy(() -> client.searchByName("b"))
                .isInstanceOf(RestClientResponseException.class);
        server.verify();
    }

    @Test
    void findById_mapsObject() {
        server.expect(once(), requestTo(containsString("/pops/44")))
                .andExpect(header("x-api-key", "test-key"))
                .andRespond(withSuccess("""
                        {"id":44,"name":"Batman","series":"DC Comics","number":"01","category":"Comics",
                        "vaulted":true,"release_year":2012,"rarity_score":30}
                        """, MediaType.APPLICATION_JSON));

        Optional<FunkoSearchResult> result = client.findById("44");

        assertThat(result).isPresent();
        assertThat(result.get().externalId()).isEqualTo("44");
        assertThat(result.get().name()).isEqualTo("Batman");
        assertThat(result.get().vaulted()).isTrue();
    }

    @Test
    void findById_returnsEmpty_on404_withoutRetry() {
        server.expect(once(), requestTo(containsString("/pops/999999")))
                .andRespond(withStatus(HttpStatus.NOT_FOUND));

        assertThat(client.findById("999999")).isEmpty();
        server.verify();
    }

    @Test
    void findById_retries5xx_thenRethrows() {
        server.expect(times(4), requestTo(containsString("/pops/44")))
                .andRespond(withServerError());

        assertThatThrownBy(() -> client.findById("44"))
                .isInstanceOf(RestClientResponseException.class);
        server.verify();
    }

    @Test
    void listCategories_mapsArray() {
        server.expect(once(), requestTo(containsString("/categories")))
                .andExpect(header("x-api-key", "test-key"))
                .andRespond(withSuccess("""
                        [{"name":"Television","count":1385},{"name":"Movies","count":1238}]
                        """, MediaType.APPLICATION_JSON));

        List<FunkoCategory> categories = client.listCategories();

        assertThat(categories).containsExactly(
                new FunkoCategory("Television", 1385),
                new FunkoCategory("Movies", 1238));
    }

    @Test
    void listCategories_returnsEmpty_whenNull() {
        server.expect(once(), requestTo(containsString("/categories")))
                .andRespond(withSuccess("null", MediaType.APPLICATION_JSON));

        assertThat(client.listCategories()).isEmpty();
    }
}
