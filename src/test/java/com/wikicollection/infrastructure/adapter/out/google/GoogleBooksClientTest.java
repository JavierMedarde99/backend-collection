package com.wikicollection.infrastructure.adapter.out.google;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.ExpectedCount.manyTimes;
import static org.springframework.test.web.client.ExpectedCount.once;
import static org.springframework.test.web.client.ExpectedCount.times;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.anything;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import java.util.List;

import com.wikicollection.domain.model.BookSearchResult;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

class GoogleBooksClientTest {

    private MockRestServiceServer server;
    private GoogleBooksClient client;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder()
                .baseUrl("https://www.googleapis.com/books");
        server = MockRestServiceServer.bindTo(builder).build();
        client = new GoogleBooksClient(builder.build(), "", 0);
    }

    @Test
    void search_mapsGoogleBooksResponse() {
        server.expect(once(), requestTo("https://www.googleapis.com/books/v1/volumes?q=intitle:cien&maxResults=10&langRestrict=es"))
                .andRespond(withSuccess(googleBooksFixture(), MediaType.APPLICATION_JSON));

        List<BookSearchResult> results = client.search("cien");

        assertThat(results).hasSize(1);
        BookSearchResult result = results.get(0);
        assertThat(result.id()).isEqualTo("abc123");
        assertThat(result.title()).isEqualTo("Cien años de soledad");
        assertThat(result.authors()).containsExactly("Gabriel García Márquez");
        assertThat(result.isbn()).isEqualTo("9780307474728");
        assertThat(result.coverImage()).isEqualTo("http://books.google.com/books/content?id=abc&thumb.jpg");
        assertThat(result.description()).isNotEmpty();
        assertThat(result.pageCount()).isEqualTo(417);
        assertThat(result.publisher()).isEqualTo("Vintage Español");
        assertThat(result.publishedDate()).isEqualTo("2011-05-03");
        assertThat(result.language()).isEqualTo("es");
        assertThat(result.categories()).containsExactly("Literatura");
    }

    @Test
    void search_includesApiKey_whenConfigured() {
        RestClient.Builder keyBuilder = RestClient.builder()
                .baseUrl("https://www.googleapis.com/books");
        MockRestServiceServer keyServer = MockRestServiceServer.bindTo(keyBuilder).build();
        GoogleBooksClient clientWithKey = new GoogleBooksClient(keyBuilder.build(), "my-secret-key", 0);
        keyServer.expect(once(),
                        requestTo("https://www.googleapis.com/books/v1/volumes?q=intitle:test&maxResults=10&langRestrict=es&key=my-secret-key"))
                .andRespond(withSuccess(googleBooksFixture(), MediaType.APPLICATION_JSON));

        List<BookSearchResult> results = clientWithKey.search("test");

        assertThat(results).hasSize(1);
        keyServer.verify();
    }

    @Test
    void search_returnsEmpty_whenNoItems() {
        server.expect(once(), requestTo("https://www.googleapis.com/books/v1/volumes?q=intitle:vacio&maxResults=10&langRestrict=es"))
                .andRespond(withSuccess("{}", MediaType.APPLICATION_JSON));

        List<BookSearchResult> results = client.search("vacio");

        assertThat(results).isEmpty();
    }

    @Test
    void search_throwsWhenAllRetriesFail() {
        server.expect(times(4), anything()).andRespond(withServerError());

        assertThatThrownBy(() -> client.search("cien"))
                .isInstanceOf(RestClientResponseException.class);
        server.verify();
    }

    @Test
    void search_succeedsAfterRetry_whenTransientFailure() {
        server.expect(once(), anything()).andRespond(withServerError());
        server.expect(once(), anything()).andRespond(withSuccess(googleBooksFixture(), MediaType.APPLICATION_JSON));

        List<BookSearchResult> results = client.search("cien");

        assertThat(results).hasSize(1);
        server.verify();
    }

    private String googleBooksFixture() {
        return """
                {
                  "totalItems": 1,
                  "items": [
                    {
                      "id": "abc123",
                      "volumeInfo": {
                        "title": "Cien años de soledad",
                        "authors": ["Gabriel García Márquez"],
                        "publisher": "Vintage Español",
                        "publishedDate": "2011-05-03",
                        "description": "Una obra maestra de la literatura.",
                        "pageCount": 417,
                        "categories": ["Literatura"],
                        "language": "es",
                        "imageLinks": {
                          "thumbnail": "http://books.google.com/books/content?id=abc&thumb.jpg"
                        },
                        "industryIdentifiers": [
                          { "type": "ISBN_13", "identifier": "9780307474728" },
                          { "type": "ISBN_10", "identifier": "0307474728" }
                        ]
                      }
                    }
                  ]
                }
                """;
    }
}