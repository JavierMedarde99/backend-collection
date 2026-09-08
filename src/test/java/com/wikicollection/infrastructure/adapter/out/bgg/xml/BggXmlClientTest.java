package com.wikicollection.infrastructure.adapter.out.bgg.xml;

import static org.assertj.core.api.Assertions.assertThat;

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
import org.springframework.web.client.RestTemplate;

class BggXmlClientTest {

    private MockWebServer server;
    private BggXmlClient client;

    @BeforeEach
    void setUp() throws Exception {
        server = new MockWebServer();
        server.start();
        client = new BggXmlClient(new RestTemplate(), server.url("/xmlapi2").toString(), 1, 0, new BoardGameXmlMapper());
    }

    @AfterEach
    void tearDown() throws Exception {
        server.shutdown();
    }

    @Test
    void search_mapsBggXmlResponse() throws Exception {
        server.enqueue(new MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", MediaType.APPLICATION_XML_VALUE)
                .setBody(bggXmlFixture()));

        List<BoardGameSearchResult> results = client.search("catan");

        assertThat(results).hasSize(1);
        BoardGameSearchResult result = results.get(0);
        assertThat(result.bggId()).isEqualTo("31260");
        assertThat(result.title()).isEqualTo("Catan");
        assertThat(result.yearPublished()).isEqualTo(2007);
        assertThat(result.imageUrl()).isEqualTo("http://img");
        assertThat(result.thumbnailUrl()).isEqualTo("http://thumb");
        assertThat(result.externalSource()).isEqualTo("BGG");

        RecordedRequest request = server.takeRequest();
        assertThat(request.getPath()).startsWith("/xmlapi2/search?query=catan&type=boardgame");
    }

    @Test
    void search_retriesWhenAccepted_andReturnsResultsOnSuccess() throws Exception {
        server.enqueue(new MockResponse()
                .setResponseCode(202)
                .setHeader(HttpHeaders.SET_COOKIE, "BGGCF=abc123; Path=/; Secure")
                .setBody(""));
        server.enqueue(new MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", MediaType.APPLICATION_XML_VALUE)
                .setBody(bggXmlFixture()));

        List<BoardGameSearchResult> results = client.search("catan");

        assertThat(results).hasSize(1);
        assertThat(results.get(0).bggId()).isEqualTo("31260");

        RecordedRequest first = server.takeRequest();
        assertThat(first.getPath()).startsWith("/xmlapi2/search?query=catan");
        assertThat(first.getHeader(HttpHeaders.COOKIE)).isNull();

        RecordedRequest second = server.takeRequest();
        assertThat(second.getHeader(HttpHeaders.COOKIE)).isEqualTo("BGGCF=abc123");
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

    private String bggXmlFixture() {
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
}