package com.wikicollection.infrastructure.adapter.out.bgg.xml;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.List;

import com.wikicollection.domain.model.BoardGameSearchResult;
import com.wikicollection.infrastructure.adapter.out.bgg.mapper.BoardGameXmlMapper;

import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.web.client.RestClient;

class BggXmlClientTest {

    private MockWebServer server;
    private BggXmlClient client;

    @BeforeEach
    void setUp() throws Exception {
        server = new MockWebServer();
        server.start();
        client = new BggXmlClient(RestClient.builder().baseUrl(server.url("/xmlapi2").toString()).build(), server.url("/xmlapi2").toString(), 1, 0, new BoardGameXmlMapper());
    }

    @AfterEach
    void tearDown() throws Exception {
        server.shutdown();
    }

    @Test
    void search_mapsBggXmlResponseAndEnrichesWithThingDetails() throws Exception {
        server.enqueue(new MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", MediaType.APPLICATION_XML_VALUE + ";charset=UTF-8")
                .setBody(bggSearchFixture()));
        server.enqueue(new MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", MediaType.APPLICATION_XML_VALUE + ";charset=UTF-8")
                .setBody(bggThingFixture()));

        List<BoardGameSearchResult> results = client.search("catan");

        assertThat(results).hasSize(1);
        BoardGameSearchResult result = results.get(0);
        assertThat(result.bggId()).isEqualTo("31260");
        assertThat(result.title()).isEqualTo("Catan");
        assertThat(result.yearPublished()).isEqualTo(2007);
        assertThat(result.imageUrl()).isEqualTo("http://img");
        assertThat(result.thumbnailUrl()).isEqualTo("http://thumb");
        assertThat(result.description()).isEqualTo("Un juego de colonización");
        assertThat(result.minPlayers()).isEqualTo(3);
        assertThat(result.maxPlayers()).isEqualTo(4);
        assertThat(result.minPlaytime()).isEqualTo(60);
        assertThat(result.maxPlaytime()).isEqualTo(120);
        assertThat(result.publisher()).isEqualTo("Kosmos");
        assertThat(result.designers()).containsExactly("Klaus Teuber");
        assertThat(result.categories()).containsExactly("Negociación", "Estrategia");
        assertThat(result.mechanics()).containsExactly("Dados", "Colocación de losetas");
        assertThat(result.bggRating()).isEqualByComparingTo(new BigDecimal("8.3"));
        assertThat(result.externalSource()).isEqualTo("BGG");

        RecordedRequest searchRequest = server.takeRequest();
        assertThat(searchRequest.getPath()).startsWith("/xmlapi2/search?query=catan&type=boardgame");
        RecordedRequest thingRequest = server.takeRequest();
        assertThat(thingRequest.getPath()).startsWith("/xmlapi2/thing?id=31260&stats=1");
    }

    @Test
    void search_returnsBasicResults_whenThingCallFails() throws Exception {
        server.enqueue(new MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", MediaType.APPLICATION_XML_VALUE + ";charset=UTF-8")
                .setBody(bggSearchFixture()));
        server.enqueue(new MockResponse().setResponseCode(500));

        List<BoardGameSearchResult> results = client.search("catan");

        assertThat(results).hasSize(1);
        assertThat(results.get(0).bggId()).isEqualTo("31260");
        assertThat(results.get(0).description()).isNull();
        assertThat(results.get(0).bggRating()).isNull();
    }

    @Test
    void search_returnsEmpty_whenThingReturnsEmptyDetails() throws Exception {
        server.enqueue(new MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", MediaType.APPLICATION_XML_VALUE + ";charset=UTF-8")
                .setBody(bggSearchFixture()));
        server.enqueue(new MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", MediaType.APPLICATION_XML_VALUE + ";charset=UTF-8")
                .setBody("<items total=\"0\"></items>"));

        List<BoardGameSearchResult> results = client.search("catan");

        assertThat(results).hasSize(1);
        assertThat(results.get(0).description()).isNull();
        assertThat(results.get(0).bggRating()).isNull();
    }

    @Test
    void search_returnsEmpty_whenSearchEmpty_skipsThingCall() throws Exception {
        server.enqueue(new MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", MediaType.APPLICATION_XML_VALUE + ";charset=UTF-8")
                .setBody("<items total=\"0\"></items>"));

        List<BoardGameSearchResult> results = client.search("catan");

        assertThat(results).isEmpty();
        assertThat(server.getRequestCount()).isEqualTo(1);
    }

    @Test
    void search_retriesWhenAccepted_andReturnsResultsOnSuccess() throws Exception {
        server.enqueue(new MockResponse()
                .setResponseCode(202)
                .setHeader(HttpHeaders.SET_COOKIE, "BGGCF=abc123; Path=/; Secure")
                .setBody(""));
        server.enqueue(new MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", MediaType.APPLICATION_XML_VALUE + ";charset=UTF-8")
                .setBody(bggSearchFixture()));
        server.enqueue(new MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", MediaType.APPLICATION_XML_VALUE + ";charset=UTF-8")
                .setBody(bggThingFixture()));

        List<BoardGameSearchResult> results = client.search("catan");

        assertThat(results).hasSize(1);
        assertThat(results.get(0).bggId()).isEqualTo("31260");
        assertThat(results.get(0).description()).isEqualTo("Un juego de colonización");

        RecordedRequest first = server.takeRequest();
        assertThat(first.getPath()).startsWith("/xmlapi2/search?query=catan");
        assertThat(first.getHeader(HttpHeaders.COOKIE)).isNull();

        RecordedRequest second = server.takeRequest();
        assertThat(second.getHeader(HttpHeaders.COOKIE)).isEqualTo("BGGCF=abc123");

        server.takeRequest();
    }

    @Test
    void search_returnsEmpty_whenAlwaysAccepted() throws Exception {
        server.enqueue(new MockResponse().setResponseCode(202).setBody(""));
        server.enqueue(new MockResponse().setResponseCode(202).setBody(""));

        List<BoardGameSearchResult> results = client.search("catan");

        assertThat(results).isEmpty();
        assertThat(server.getRequestCount()).isEqualTo(2);
    }

    @Test
    void search_returnsEmpty_whenServerError() throws Exception {
        server.enqueue(new MockResponse().setResponseCode(500));

        List<BoardGameSearchResult> results = client.search("catan");

        assertThat(results).isEmpty();
    }

    @Test
    void search_returnsEmpty_whenConnectionFails() throws Exception {
        server.shutdown();

        List<BoardGameSearchResult> results = client.search("catan");

        assertThat(results).isEmpty();
    }

    @Test
    void search_sendsBearerTokenInBothSearchAndThingCalls() throws Exception {
        ClientHttpRequestInterceptor interceptor = (request, body, execution) -> {
            request.getHeaders().setBearerAuth("test-token");
            return execution.execute(request, body);
        };
        RestClient authenticatedRestClient = RestClient.builder()
                .baseUrl(server.url("/xmlapi2").toString())
                .requestInterceptor(interceptor)
                .build();
        BggXmlClient authenticatedClient = new BggXmlClient(authenticatedRestClient, server.url("/xmlapi2").toString(), 1, 0, new BoardGameXmlMapper());
        server.enqueue(new MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", MediaType.APPLICATION_XML_VALUE + ";charset=UTF-8")
                .setBody(bggSearchFixture()));
        server.enqueue(new MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", MediaType.APPLICATION_XML_VALUE + ";charset=UTF-8")
                .setBody(bggThingFixture()));

        authenticatedClient.search("catan");

        RecordedRequest searchRequest = server.takeRequest();
        assertThat(searchRequest.getHeader(HttpHeaders.AUTHORIZATION)).isEqualTo("Bearer test-token");
        RecordedRequest thingRequest = server.takeRequest();
        assertThat(thingRequest.getHeader(HttpHeaders.AUTHORIZATION)).isEqualTo("Bearer test-token");
    }

    @Test
    void search_limitsThingIdsToTwenty_whenManySearchResults() throws Exception {
        server.enqueue(new MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", MediaType.APPLICATION_XML_VALUE + ";charset=UTF-8")
                .setBody(bggSearchFixtureManyResults()));
        server.enqueue(new MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", MediaType.APPLICATION_XML_VALUE + ";charset=UTF-8")
                .setBody(bggThingFixture()));

        List<BoardGameSearchResult> results = client.search("catan");

        server.takeRequest();
        RecordedRequest thingRequest = server.takeRequest();
        assertThat(thingRequest.getPath()).startsWith("/xmlapi2/thing?id=");
        String idParam = thingRequest.getRequestUrl().queryParameter("id");
        assertThat(idParam).isNotNull();
        assertThat(idParam.split(",").length).isLessThanOrEqualTo(20);
        assertThat(results).hasSize(25);
        assertThat(results.get(0).description()).isEqualTo("Un juego de colonización");
    }

    private String bggSearchFixture() {
        return """
                <?xml version="1.0" encoding="utf-8"?>
                <items termsofuse="https://boardgamegeek.com/xmlapi/termsofuse" total="1">
                  <item type="boardgame" id="31260">
                    <name type="primary" sortindex="1" value="Catan"/>
                    <yearpublished value="2007"/>
                    <image>http://img</image>
                    <thumbnail>http://thumb</thumbnail>
                  </item>
                </items>
                """;
    }

    private String bggSearchFixtureManyResults() {
        StringBuilder sb = new StringBuilder("""
                <?xml version="1.0" encoding="utf-8"?>
                <items termsofuse="https://boardgamegeek.com/xmlapi/termsofuse" total="25">
                """);
        for (int i = 1; i <= 25; i++) {
            int id = i == 1 ? 31260 : i;
            sb.append("""
                      <item type="boardgame" id="%d">
                        <name type="primary" sortindex="1" value="Catan %d"/>
                        <yearpublished value="2000"/>
                        <image>http://img%d</image>
                        <thumbnail>http://thumb%d</thumbnail>
                      </item>
                    """.formatted(id, i, i, i));
        }
        sb.append("</items>");
        return sb.toString();
    }

    private String bggThingFixture() {
        return """
                <?xml version="1.0" encoding="utf-8"?>
                <items termsofuse="https://boardgamegeek.com/xmlapi/termsofuse">
                  <item type="boardgame" id="31260">
                    <name type="primary" sortindex="1" value="Catan"/>
                    <name type="alternate" sortindex="1" value="Los Colonos de Catan"/>
                    <description>Un juego de colonización</description>
                    <yearpublished value="2007"/>
                    <minplayers value="3"/>
                    <maxplayers value="4"/>
                    <minplaytime value="60"/>
                    <maxplaytime value="120"/>
                    <image>http://img</image>
                    <thumbnail>http://thumb</thumbnail>
                    <link type="boardgamepublisher" id="253" value="Kosmos"/>
                    <link type="boardgamedesigner" id="20" value="Klaus Teuber"/>
                    <link type="boardgamecategory" id="1028" value="Negociación"/>
                    <link type="boardgamecategory" id="1017" value="Estrategia"/>
                    <link type="boardgamemechanic" id="2011" value="Dados"/>
                    <link type="boardgamemechanic" id="2008" value="Colocación de losetas"/>
                    <statistics page="1">
                      <ratings>
                        <usersrated value="50000"/>
                        <average value="8.3"/>
                        <bayesaverage value="7.9"/>
                        <ranks>
                          <rank type="subtype" id="1" name="boardgame" friendlyname="Board Game Rank" value="42" bayesaverage="7.9"/>
                        </ranks>
                      </ratings>
                    </statistics>
                  </item>
                </items>
                """;
    }
}