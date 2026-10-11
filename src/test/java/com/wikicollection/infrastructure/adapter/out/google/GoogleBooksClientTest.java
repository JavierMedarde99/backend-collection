package com.wikicollection.infrastructure.adapter.out.google;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
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
        server.expect(once(), requestTo("https://www.googleapis.com/books/v1/volumes?q=intitle:cien&langRestrict=es&startIndex=0&maxResults=10"))
                .andRespond(withSuccess(googleBooksFixture(), MediaType.APPLICATION_JSON));

        Page<BookSearchResult> results = client.search("cien", PageRequest.of(0, 10));

        assertThat(results.getContent()).hasSize(1);
        assertThat(results.getTotalElements()).isEqualTo(1);
        BookSearchResult result = results.getContent().get(0);
        assertThat(result.id()).isEqualTo("abc123");
        assertThat(result.title()).isEqualTo("Cien años de soledad");
        assertThat(result.authors()).containsExactly("Gabriel García Márquez");
        assertThat(result.isbn()).isEqualTo("9780307474728");
        assertThat(result.coverImage()).isEqualTo("http://books.google.com/books/content?id=abc&thumb.jpg");
        assertThat(result.description()).isNotEmpty();
        assertThat(result.pageCount()).isEqualTo(417);
        assertThat(result.publisher()).isEqualTo("Vintage Español");
        assertThat(result.publishedDate()).isEqualTo("2011-05-03");
        assertThat(result.publicationYear()).isEqualTo(2011);
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
                        requestTo("https://www.googleapis.com/books/v1/volumes?q=intitle:test&langRestrict=es&startIndex=0&maxResults=10&key=my-secret-key"))
                .andRespond(withSuccess(googleBooksFixture(), MediaType.APPLICATION_JSON));

        Page<BookSearchResult> results = clientWithKey.search("test", PageRequest.of(0, 10));

        assertThat(results.getContent()).hasSize(1);
        keyServer.verify();
    }

    @Test
    void search_returnsEmpty_whenNoItems() {
        server.expect(once(), requestTo("https://www.googleapis.com/books/v1/volumes?q=intitle:vacio&langRestrict=es&startIndex=0&maxResults=10"))
                .andRespond(withSuccess("{}", MediaType.APPLICATION_JSON));

        Page<BookSearchResult> results = client.search("vacio", PageRequest.of(0, 10));

        assertThat(results).isEmpty();
    }

    @Test
    void searchByIsbn_queriesIsbnEndpoint() {
        server.expect(once(), requestTo("https://www.googleapis.com/books/v1/volumes?q=isbn:9788498382671"))
                .andRespond(withSuccess(googleBooksFixture(), MediaType.APPLICATION_JSON));

        List<BookSearchResult> results = client.searchByIsbn("9788498382671");

        assertThat(results).hasSize(1);
        assertThat(results.get(0).isbn()).isEqualTo("9780307474728");
        server.verify();
    }

    @Test
    void searchByIsbn_returnsEmpty_whenNoItems() {
        server.expect(once(), requestTo("https://www.googleapis.com/books/v1/volumes?q=isbn:9788498382671"))
                .andRespond(withSuccess("{}", MediaType.APPLICATION_JSON));

        List<BookSearchResult> results = client.searchByIsbn("9788498382671");

        assertThat(results).isEmpty();
        server.verify();
    }
    @Test
    void search_throwsWhenAllRetriesFail() {
        server.expect(times(4), anything()).andRespond(withServerError());

        assertThatThrownBy(() -> client.search("cien", PageRequest.of(0, 10)))
                .isInstanceOf(RestClientResponseException.class);
        server.verify();
    }

    @Test
    void search_succeedsAfterRetry_whenTransientFailure() {
        server.expect(once(), anything()).andRespond(withServerError());
        server.expect(once(), anything()).andRespond(withSuccess(googleBooksFixture(), MediaType.APPLICATION_JSON));

        Page<BookSearchResult> results = client.search("cien", PageRequest.of(0, 10));

        assertThat(results.getContent()).hasSize(1);
        server.verify();
    }

    @Test
    void search_usesStartIndexAndKeepsTotal_whenRequestingSecondPage() {
        server.expect(once(), requestTo("https://www.googleapis.com/books/v1/volumes?q=intitle:cien&langRestrict=es&startIndex=10&maxResults=10"))
                .andRespond(withSuccess(googleBooksFixtureWithItems(1, 25), MediaType.APPLICATION_JSON));

        Page<BookSearchResult> results = client.search("cien", PageRequest.of(1, 10));

        assertThat(results.getNumber()).isEqualTo(1);
        assertThat(results.getContent()).hasSize(1);
        assertThat(results.getTotalElements()).isEqualTo(25);
        server.verify();
    }

    @Test
    void search_paginatesIn40ItemWindows_whenSizeExceedsGoogleLimit() {
        server.expect(once(), requestTo("https://www.googleapis.com/books/v1/volumes?q=intitle:cien&langRestrict=es&startIndex=0&maxResults=40"))
                .andRespond(withSuccess(googleBooksFixtureWithItems(40, 60), MediaType.APPLICATION_JSON));
        server.expect(once(), requestTo("https://www.googleapis.com/books/v1/volumes?q=intitle:cien&langRestrict=es&startIndex=40&maxResults=10"))
                .andRespond(withSuccess(googleBooksFixtureWithItems(3, 60), MediaType.APPLICATION_JSON));

        Page<BookSearchResult> results = client.search("cien", PageRequest.of(0, 50));

        assertThat(results.getContent()).hasSize(43);
        assertThat(results.getTotalElements()).isEqualTo(60);
        server.verify();
    }

    @Test
    void search_derivesPublicationYear_fromFullDate() {
        assertThat(searchWithPublishedDate("2011-05-03").publicationYear()).isEqualTo(2011);
    }

    @Test
    void search_derivesPublicationYear_whenOnlyYearIsPublished() {
        assertThat(searchWithPublishedDate("1967").publicationYear()).isEqualTo(1967);
    }

    @Test
    void search_derivesPublicationYear_whenPublishedDateHasYearAndMonth() {
        assertThat(searchWithPublishedDate("2011-05").publicationYear()).isEqualTo(2011);
    }

    @Test
    void search_keepsPublishedDate_intact() {
        assertThat(searchWithPublishedDate("2011-05-03").publishedDate()).isEqualTo("2011-05-03");
    }

    @Test
    void search_returnsNullPublicationYear_whenPublishedDateMissing() {
        assertThat(searchWithPublishedDate(null).publicationYear()).isNull();
    }

    @Test
    void search_returnsNullPublicationYear_whenPublishedDateIsNotAYear() {
        assertThat(searchWithPublishedDate("s. l.").publicationYear()).isNull();
    }

    @Test
    void search_returnsNullPublicationYear_whenYearIsTooShort() {
        assertThat(searchWithPublishedDate("201").publicationYear()).isNull();
    }

    @Test
    void search_returnsNullPublicationYear_whenYearIsZero() {
        assertThat(searchWithPublishedDate("0000").publicationYear()).isNull();
    }

    @Test
    void search_returnsNullPublicationYear_whenMoreThanFourDigits() {
        assertThat(searchWithPublishedDate("12345").publicationYear()).isNull();
    }

    private BookSearchResult searchWithPublishedDate(String publishedDate) {
        server.expect(once(), anything())
                .andRespond(withSuccess(fixtureWithPublishedDate(publishedDate), MediaType.APPLICATION_JSON));

        Page<BookSearchResult> results = client.search("cien", PageRequest.of(0, 10));

        assertThat(results.getContent()).hasSize(1);
        return results.getContent().get(0);
    }

    private String fixtureWithPublishedDate(String publishedDate) {
        if (publishedDate == null) {
            return googleBooksFixture().replace("\"publishedDate\": \"2011-05-03\",", "");
        }
        return googleBooksFixture().replace("2011-05-03", publishedDate);
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

    private String googleBooksFixtureWithItems(int count, int totalItems) {
        StringBuilder json = new StringBuilder();
        json.append("{\"totalItems\": ").append(totalItems).append(", \"items\": [");
        for (int i = 0; i < count; i++) {
            if (i > 0) {
                json.append(',');
            }
            json.append("{\"id\": \"id").append(i)
                    .append("\", \"volumeInfo\": {\"title\": \"Titulo ").append(i).append("\"}}");
        }
        json.append("]}");
        return json.toString();
    }
}