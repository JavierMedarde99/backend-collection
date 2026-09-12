package com.wikicollection.infrastructure.adapter.in.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;

import com.wikicollection.domain.model.Book;
import com.wikicollection.domain.model.BookSearchCriteria;
import com.wikicollection.domain.model.BookSearchResult;
import com.wikicollection.domain.model.BookState;
import com.wikicollection.domain.model.BookType;
import com.wikicollection.domain.port.out.BookRepository;
import com.wikicollection.infrastructure.adapter.out.google.GoogleBooksClient;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientResponseException;

@SpringBootTest(properties = {"spring.data.mongodb.auto-index-creation=false", "app.boardgame-status-migration.enabled=false"})
@AutoConfigureMockMvc
class BookControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private BookRepository bookRepository;

    @MockitoBean
    private GoogleBooksClient googleBooksClient;

    private Book sampleBook() {
        return Book.builder()
                .id("b1")
                .title("Cien años de soledad")
                .author("Gabriel García Márquez")
                .state(BookState.TO_READ)
                .type(BookType.NOVEL)
                .build();
    }

    @Test
    void listBooks_returnsEmptyPage_whenNoBooks() throws Exception {
        when(bookRepository.search(any(BookSearchCriteria.class), any(Pageable.class))).thenReturn(Page.empty());

        mockMvc.perform(get("/api/books"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content").isEmpty());
    }

    @Test
    void listBooks_filtersByState() throws Exception {
        when(bookRepository.search(any(BookSearchCriteria.class), any(Pageable.class))).thenReturn(Page.empty());

        mockMvc.perform(get("/api/books").param("state", "READING"))
                .andExpect(status().isOk());

        ArgumentCaptor<BookSearchCriteria> captor = ArgumentCaptor.forClass(BookSearchCriteria.class);
        verify(bookRepository).search(captor.capture(), any(Pageable.class));
        org.assertj.core.api.Assertions.assertThat(captor.getValue().state()).isEqualTo(BookState.READING);
    }

    @Test
    void listBooks_returns400_whenNameTooLong() throws Exception {
        mockMvc.perform(get("/api/books").param("name", "a".repeat(101)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    void listBooks_filtersByNameAuthorAndType() throws Exception {
        when(bookRepository.search(any(BookSearchCriteria.class), any(Pageable.class))).thenReturn(Page.empty());

        mockMvc.perform(get("/api/books")
                        .param("name", "cien")
                        .param("author", "garcía")
                        .param("type", "NOVEL"))
                .andExpect(status().isOk());

        ArgumentCaptor<BookSearchCriteria> captor = ArgumentCaptor.forClass(BookSearchCriteria.class);
        verify(bookRepository).search(captor.capture(), any(Pageable.class));
        BookSearchCriteria criteria = captor.getValue();
        org.assertj.core.api.Assertions.assertThat(criteria.name()).isEqualTo("cien");
        org.assertj.core.api.Assertions.assertThat(criteria.author()).isEqualTo("garcía");
        org.assertj.core.api.Assertions.assertThat(criteria.type()).isEqualTo(BookType.NOVEL);
    }

    @Test
    void getBook_returnsBook_whenExists() throws Exception {
        when(bookRepository.findById("b1")).thenReturn(Optional.of(sampleBook()));

        mockMvc.perform(get("/api/books/b1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("b1"))
                .andExpect(jsonPath("$.title").value("Cien años de soledad"))
                .andExpect(jsonPath("$.state").value("TO_READ"));
    }

    @Test
    void getBook_returns404_whenMissing() throws Exception {
        when(bookRepository.findById("nope")).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/books/nope"))
                .andExpect(status().isNotFound());
    }

    @Test
    void createBook_returns201_withLocation() throws Exception {
        when(bookRepository.save(any(Book.class))).thenAnswer(invocation -> {
            Book saved = invocation.getArgument(0);
            saved.setId("b-new");
            return saved;
        });

        mockMvc.perform(post("/api/books")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":"Cien años de soledad","author":"Gabriel García Márquez","state":"TO_READ","type":"NOVEL"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", Matchers.containsString("/api/books/b-new")))
                .andExpect(jsonPath("$.id").value("b-new"))
                .andExpect(jsonPath("$.title").value("Cien años de soledad"));
    }

    @Test
    void createBook_returns400_whenInvalid() throws Exception {
        mockMvc.perform(post("/api/books")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":"","author":"","state":"TO_READ","type":"NOVEL"}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createBook_returns409_whenExternalIdAlreadyExists() throws Exception {
        Book existing = sampleBook();
        existing.setExternalId("gb123");
        when(bookRepository.findByExternalId("gb123")).thenReturn(Optional.of(existing));

        mockMvc.perform(post("/api/books")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":"Cien años de soledad","author":"Gabriel","externalId":"gb123","state":"TO_READ","type":"NOVEL"}
                                """))
                .andExpect(status().isConflict());
    }

    @Test
    void createBook_returns400_whenStartOutOfRange() throws Exception {
        mockMvc.perform(post("/api/books")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":"Cien años","author":"G.G.M.","state":"TO_READ","type":"NOVEL","start":9}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createBook_returns400_whenStartDateIsFuture() throws Exception {
        mockMvc.perform(post("/api/books")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":"Cien años","author":"G.G.M.","state":"TO_READ","type":"NOVEL","startDate":"2099-01-01"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("Fechas mal formadas")));
    }

    @Test
    void createBook_returns400_whenStartAfterEnd() throws Exception {
        mockMvc.perform(post("/api/books")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":"Cien años","author":"G.G.M.","state":"TO_READ","type":"NOVEL","startDate":"2024-06-01","endDate":"2024-01-01"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("Fechas mal formadas")));
    }

    @Test
    void updateBook_returns400_whenEndDateIsFuture() throws Exception {
        when(bookRepository.findById("b1")).thenReturn(Optional.of(sampleBook()));

        mockMvc.perform(put("/api/books/b1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":"Nuevo título","author":"Autor Actualizado","state":"COMPLETED","type":"NOVEL","endDate":"2099-01-01"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("Fechas mal formadas")));
    }

    @Test
    void updateBook_returnsUpdatedBook() throws Exception {
        when(bookRepository.findById("b1")).thenReturn(Optional.of(sampleBook()));
        when(bookRepository.save(any(Book.class))).thenAnswer(invocation -> invocation.getArgument(0));

        mockMvc.perform(put("/api/books/b1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":"Nuevo título","author":"Autor Actualizado","state":"COMPLETED","type":"NOVEL","pages":300}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Nuevo título"))
                .andExpect(jsonPath("$.author").value("Autor Actualizado"))
                .andExpect(jsonPath("$.state").value("COMPLETED"))
                .andExpect(jsonPath("$.pages").value(300));
    }

    @Test
    void deleteBook_returns204_whenExists() throws Exception {
        when(bookRepository.findById("b1")).thenReturn(Optional.of(sampleBook()));

        mockMvc.perform(delete("/api/books/b1"))
                .andExpect(status().isNoContent());

        verify(bookRepository).deleteById("b1");
    }

    @Test
    void deleteBook_returns404_whenMissing() throws Exception {
        when(bookRepository.findById("nope")).thenReturn(Optional.empty());

        mockMvc.perform(delete("/api/books/nope"))
                .andExpect(status().isNotFound());
    }

    @Test
    void search_returnsGoogleBooksResults() throws Exception {
        BookSearchResult result = new BookSearchResult(
                "abc123", "Cien años de soledad", List.of("Gabriel García Márquez"),
                "9780307474728", "http://thumb", "Sinopsis", 417,
                "Vintage Español", "2011-05-03", "es", List.of("Literatura"));
        when(googleBooksClient.search("cien")).thenReturn(List.of(result));

        mockMvc.perform(get("/api/books/search").param("name", "cien"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].title").value("Cien años de soledad"))
                .andExpect(jsonPath("$[0].isbn").value("9780307474728"));
    }

    @Test
    void search_returns400_whenBlankQuery() throws Exception {
        mockMvc.perform(get("/api/books/search").param("name", " "))
                .andExpect(status().isBadRequest());
    }

    @Test
    void search_returnsEmpty_whenGoogleUnavailable() throws Exception {
        when(googleBooksClient.search("cien")).thenReturn(List.of());

        mockMvc.perform(get("/api/books/search").param("name", "cien"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());
    }

    @Test
    void search_returns502_whenGoogleThrowsRestClientError() throws Exception {
        when(googleBooksClient.search("cien")).thenThrow(
                new RestClientResponseException(
                        "error", 500, "Internal Server Error", HttpHeaders.EMPTY, new byte[0], StandardCharsets.UTF_8));

        mockMvc.perform(get("/api/books/search").param("name", "cien"))
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.status").value(502));
    }

    @Test
    void search_returns503_whenGoogleUnreachable() throws Exception {
        when(googleBooksClient.search("cien")).thenThrow(new ResourceAccessException("no disponible"));

        mockMvc.perform(get("/api/books/search").param("name", "cien"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.status").value(503));
    }

    @Test
    void cors_allowsFrontendOrigin() throws Exception {
        mockMvc.perform(options("/api/books")
                        .header("Origin", "http://localhost:5173")
                        .header("Access-Control-Request-Method", "GET"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:5173"));
    }
}
