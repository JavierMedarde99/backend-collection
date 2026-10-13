package com.wikicollection.infrastructure.adapter.out.amiibo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.client.ExpectedCount.once;
import static org.springframework.test.web.client.ExpectedCount.times;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.queryParam;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withException;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import java.io.IOException;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import com.wikicollection.domain.model.AmiiboReleaseDates;
import com.wikicollection.domain.model.AmiiboSearchResult;
import com.wikicollection.domain.model.AmiiboType;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

class AmiiboApiClientTest {

    private static final String BASE_URL = "https://amiiboapi.org/api";

    private MockRestServiceServer server;
    private AmiiboApiClient client;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder().baseUrl(BASE_URL);
        server = MockRestServiceServer.bindTo(builder).build();
        client = new AmiiboApiClient(builder.build(), 0);
    }

    @Test
    void searchByName_buildsCaseSensitiveParams() {
        server.expect(once(), requestTo(containsString("/api/amiibo")))
                .andExpect(queryParam("name", "mario"))
                .andExpect(queryParam("showusage", "true"))
                .andRespond(withSuccess("{\"amiibo\":[]}", MediaType.APPLICATION_JSON));

        List<AmiiboSearchResult> results = client.searchByName("mario");

        assertThat(results).isEmpty();
    }

    @Test
    void searchByName_mapsAllFields() {
        server.expect(once(), requestTo(containsString("/api/amiibo")))
                .andExpect(queryParam("name", "mario"))
                .andRespond(withSuccess("""
                        {"amiibo":[{"amiiboSeries":"Mario Sports Superstars","character":"Metal Mario","gameSeries":"Mario Sports Superstars","head":"09d00301","image":"https://raw.githubusercontent.com/8bitDream/AmiiboAPI/master/images/icon_09d00301-02bb0e02.png","name":"Metal Mario","release":{"au":"2017-06-16","eu":"2017-06-16","jp":"2018-07-20","na":"2017-06-16"},"tail":"02bb0e02","type":"Figure","games3DS":[{"gameName":"Mario Sports Superstars"}],"gamesSwitch":[{"gameName":"Super Smash Bros. Ultimate"}],"gamesWiiU":[],"gamesSwitch2":[]}]}
                        """, MediaType.APPLICATION_JSON));

        List<AmiiboSearchResult> results = client.searchByName("mario");

        assertThat(results).hasSize(1);
        AmiiboSearchResult r = results.get(0);
        assertThat(r.externalId()).isEqualTo("09d0030102bb0e02");
        assertThat(r.name()).isEqualTo("Metal Mario");
        assertThat(r.character()).isEqualTo("Metal Mario");
        assertThat(r.amiiboSeries()).isEqualTo("Mario Sports Superstars");
        assertThat(r.gameSeries()).isEqualTo("Mario Sports Superstars");
        assertThat(r.amiiboType()).isEqualTo(AmiiboType.FIGURE);
        assertThat(r.releaseDates()).isEqualTo(new AmiiboReleaseDates(
                LocalDate.of(2017, 6, 16),
                LocalDate.of(2017, 6, 16),
                LocalDate.of(2018, 7, 20),
                LocalDate.of(2017, 6, 16)));
        assertThat(r.imageUrl()).contains("icon_09d00301");
        assertThat(r.compatibleGames()).containsExactly("Mario Sports Superstars", "Super Smash Bros. Ultimate");
        assertThat(r.externalSource()).isEqualTo("amiiboapi");
    }

    @Test
    void searchByName_mapsUnknownTypeToNull() {
        server.expect(once(), requestTo(containsString("/api/amiibo")))
                .andRespond(withSuccess("""
                        {"amiibo":[{"head":"00000000","tail":"00000001","name":"Sparkle","type":"Sparkle"}]}
                        """, MediaType.APPLICATION_JSON));

        List<AmiiboSearchResult> results = client.searchByName("sparkle");

        assertThat(results).hasSize(1);
        assertThat(results.get(0).amiiboType()).isNull();
    }

    @Test
    void searchByName_returnsEmpty_when200EmptyList() {
        server.expect(once(), requestTo(containsString("/api/amiibo")))
                .andRespond(withSuccess("{\"amiibo\":[]}", MediaType.APPLICATION_JSON));

        List<AmiiboSearchResult> results = client.searchByName("zzzzzz");

        assertThat(results).isEmpty();
    }

    @Test
    void searchByName_returnsEmpty_whenNullContainer() {
        server.expect(once(), requestTo(containsString("/api/amiibo")))
                .andRespond(withSuccess("{\"amiibo\":null}", MediaType.APPLICATION_JSON));

        List<AmiiboSearchResult> results = client.searchByName("mario");

        assertThat(results).isEmpty();
    }

    @Test
    void searchByName_retries5xx_4Attempts_thenRethrows() {
        server.expect(times(4), requestTo(containsString("/api/amiibo")))
                .andRespond(withServerError());

        assertThatThrownBy(() -> client.searchByName("mario"))
                .isInstanceOf(RestClientResponseException.class);
        server.verify();
    }

    @Test
    void searchByName_propagatesTimeout() {
        server.expect(times(4), requestTo(containsString("/api/amiibo")))
                .andRespond(withException(new IOException("read timeout")));

        assertThatThrownBy(() -> client.searchByName("mario"))
                .isInstanceOf(ResourceAccessException.class);
        server.verify();
    }

    @Test
    void findById_mapsSingleObjectContainer() {
        server.expect(once(), requestTo(containsString("/api/amiibo")))
                .andExpect(queryParam("id", "0000000000000002"))
                .andRespond(withSuccess("""
                        {"amiibo":{"amiiboSeries":"Super Smash Bros.","character":"Mario","gameSeries":"Super Smash Bros.","head":"00000000","image":"https://raw.githubusercontent.com/8bitDream/AmiiboAPI/master/images/icon_00000000-00000002.png","name":"Mario","release":{"au":"2014-11-29","eu":"2014-11-28","jp":"2014-12-06","na":"2014-11-21"},"tail":"00000002","type":"Figure"}}
                        """, MediaType.APPLICATION_JSON));

        Optional<AmiiboSearchResult> result = client.findById("0000000000000002");

        assertThat(result).isPresent();
        assertThat(result.get().externalId()).isEqualTo("0000000000000002");
        assertThat(result.get().name()).isEqualTo("Mario");
        assertThat(result.get().amiiboType()).isEqualTo(AmiiboType.FIGURE);
    }

    @Test
    void findById_returnsEmpty_on400_withoutRetry() {
        server.expect(once(), requestTo(containsString("/api/amiibo")))
                .andExpect(queryParam("id", "zzzzzzzzzzzzzzzz"))
                .andRespond(withStatus(HttpStatus.BAD_REQUEST));

        Optional<AmiiboSearchResult> result = client.findById("zzzzzzzzzzzzzzzz");

        assertThat(result).isEmpty();
        server.verify();
    }

    @Test
    void findById_retries5xx_thenRethrows() {
        server.expect(times(4), requestTo(containsString("/api/amiibo")))
                .andRespond(withServerError());

        assertThatThrownBy(() -> client.findById("0000000000000002"))
                .isInstanceOf(RestClientResponseException.class);
        server.verify();
    }
}