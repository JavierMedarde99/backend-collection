package com.wikicollection.infrastructure.adapter.out.openlibrary;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.ExpectedCount.once;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import java.util.List;

import com.wikicollection.domain.model.BookSearchResult;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class OpenLibraryClientTest {

    private static final String EDITION_URL = "https://openlibrary.org/isbn/9788466356718.json";
    private static final String WORK_URL = "https://openlibrary.org/works/OL103123W.json";

    private MockRestServiceServer server;
    private OpenLibraryClient client;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder()
                .baseUrl("https://openlibrary.org");
        server = MockRestServiceServer.bindTo(builder).build();
        client = new OpenLibraryClient(builder.build());
    }

    private BookSearchResult resultWithoutCoverAndDescription() {
        return new BookSearchResult(
                "LCPqzgEACAAJ", "Fahrenheit 451", List.of("Ray Bradbury"),
                "9788466356718", null, null, 192,
                "DEBOLSILLO", "2021-06", 2021, "es", List.of());
    }

    @Test
    void enrich_fillsCoverAndDescription_fromOpenLibrary() {
        server.expect(once(), requestTo(EDITION_URL))
                .andRespond(withSuccess(editionWithCoverAndWork(), MediaType.APPLICATION_JSON));
        server.expect(once(), requestTo(WORK_URL))
                .andRespond(withSuccess(workWithDescription(), MediaType.APPLICATION_JSON));

        BookSearchResult enriched = client.enrich(resultWithoutCoverAndDescription());

        assertThat(enriched.coverImage()).isEqualTo("https://covers.openlibrary.org/b/id/12997704-M.jpg");
        assertThat(enriched.description())
                .isEqualTo("Fahrenheit 451 is a 1953 dystopian novel by American writer Ray Bradbury.");
        assertThat(enriched.id()).isEqualTo("LCPqzgEACAAJ");
        assertThat(enriched.title()).isEqualTo("Fahrenheit 451");
        server.verify();
    }

    @Test
    void enrich_fillsCoverOnly_whenEditionHasNoWork() {
        server.expect(once(), requestTo(EDITION_URL))
                .andRespond(withSuccess(editionWithCoverNoWork(), MediaType.APPLICATION_JSON));

        BookSearchResult enriched = client.enrich(resultWithoutCoverAndDescription());

        assertThat(enriched.coverImage()).isEqualTo("https://covers.openlibrary.org/b/id/12997704-M.jpg");
        assertThat(enriched.description()).isNull();
        server.verify();
    }

    @Test
    void enrich_readsDescription_whenTextIsStructured() {
        server.expect(once(), requestTo(EDITION_URL))
                .andRespond(withSuccess(editionWithCoverAndWork(), MediaType.APPLICATION_JSON));
        server.expect(once(), requestTo(WORK_URL))
                .andRespond(withSuccess(workWithStructuredDescription(), MediaType.APPLICATION_JSON));

        BookSearchResult enriched = client.enrich(resultWithoutCoverAndDescription());

        assertThat(enriched.description()).isEqualTo("A classic dystopian novel.");
        server.verify();
    }

    @Test
    void enrich_returnsOriginal_whenEditionNotFound() {
        server.expect(once(), requestTo(EDITION_URL))
                .andRespond(withStatus(HttpStatus.NOT_FOUND));

        BookSearchResult enriched = client.enrich(resultWithoutCoverAndDescription());

        assertThat(enriched).isEqualTo(resultWithoutCoverAndDescription());
        server.verify();
    }

    @Test
    void enrich_doesNotCallOpenLibrary_whenNothingMissing() {
        BookSearchResult complete = new BookSearchResult(
                "abc", "Dune", List.of("Frank Herbert"),
                "9788498382671", "http://thumb", "Sinopsis", 412,
                "Debolsillo", "2008-01-01", 2008, "es", List.of("Novela"));

        BookSearchResult enriched = client.enrich(complete);

        assertThat(enriched).isSameAs(complete);
    }

    private String editionWithCoverAndWork() {
        return """
                {
                  "title": "Fahrenheit 451",
                  "covers": [12997704],
                  "works": [{"key": "/works/OL103123W"}]
                }
                """;
    }

    private String editionWithCoverNoWork() {
        return """
                {
                  "title": "Fahrenheit 451",
                  "covers": [12997704]
                }
                """;
    }

    private String workWithDescription() {
        return """
                {
                  "description": "Fahrenheit 451 is a 1953 dystopian novel by American writer Ray Bradbury."
                }
                """;
    }

    private String workWithStructuredDescription() {
        return """
                {
                  "description": {"type": "/type/text", "value": "A classic dystopian novel."}
                }
                """;
    }
}