package com.wikicollection.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.ExpectedCount.once;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.anything;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import java.util.List;

import com.wikicollection.dto.GoogleBookResult;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import org.springframework.web.server.ResponseStatusException;

class GoogleBooksServiceTest {

    private MockRestServiceServer server;
    private GoogleBooksService service;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder()
                .baseUrl("https://www.googleapis.com/books");
        server = MockRestServiceServer.bindTo(builder).build();
        service = new GoogleBooksService(builder.build(), "");
    }

    @Test
    void search_mapsGoogleBooksResponse() {
        server.expect(once(), requestTo("https://www.googleapis.com/books/v1/volumes?q=cien&maxResults=20"))
                .andRespond(withSuccess(googleBooksFixture(), MediaType.APPLICATION_JSON));

        List<GoogleBookResult> results = service.search("cien");

        assertThat(results).hasSize(1);
        GoogleBookResult result = results.get(0);
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
        GoogleBooksService serviceWithKey = new GoogleBooksService(keyBuilder.build(), "my-secret-key");
        keyServer.expect(once(),
                        requestTo("https://www.googleapis.com/books/v1/volumes?q=test&maxResults=20&key=my-secret-key"))
                .andRespond(withSuccess(googleBooksFixture(), MediaType.APPLICATION_JSON));

        List<GoogleBookResult> results = serviceWithKey.search("test");

        assertThat(results).hasSize(1);
        keyServer.verify();
    }

    @Test
    void search_returnsEmpty_whenNoItems() {
        server.expect(once(), requestTo("https://www.googleapis.com/books/v1/volumes?q=vacio&maxResults=20"))
                .andRespond(withSuccess("{}", MediaType.APPLICATION_JSON));

        List<GoogleBookResult> results = service.search("vacio");

        assertThat(results).isEmpty();
    }

    @Test
    void search_throwsServerError_whenApiFails() {
        server.expect(once(), anything()).andRespond(withServerError());

        assertThatThrownBy(() -> service.search("cien"))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("500");
    }

    @Test
    void search_rejectsBlankQuery() {
        assertThatThrownBy(() -> service.search("   "))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("400");
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