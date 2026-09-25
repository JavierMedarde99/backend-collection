package com.wikicollection.infrastructure.adapter.in.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
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
import com.wikicollection.domain.model.CollectionType;
import com.wikicollection.domain.port.out.BookRepository;
import com.wikicollection.infrastructure.adapter.out.google.GoogleBooksClient;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
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
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientResponseException;

@SpringBootTest(properties = {"spring.data.mongodb.auto-index-creation=false", "app.boardgame-status-migration.enabled=false"})
@AutoConfigureMockMvc
class BookControllerTest {

    @MockitoBean
    private com.wikicollection.application.service.OwnerResolver ownerResolver;

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private BookRepository bookRepository;

    @MockitoBean
    private GoogleBooksClient googleBooksClient;

    @MockitoBean
    private com.wikicollection.domain.port.in.UserPreferencesUseCase preferencesUseCase;

    private Book sampleBook() {
        return Book.builder()
                .id("b1")
                .ownerId("u1")
                .title("Cien años de soledad")
                .author("Gabriel García Márquez")
                .state(BookState.TO_READ)
                .type(BookType.NOVEL)
                .build();
    }

    @BeforeEach
    void stubOwnerResolver() {
        lenient().when(ownerResolver.resolveOwner(org.mockito.ArgumentMatchers.any()))
                .thenAnswer(invocation -> com.wikicollection.domain.model.UserOwned.builder()
                        .ownerId(invocation.getArgument(0)).ownerName("Javi").build());
    }

    @Test
    void listBooks_mine_requiresAuth_whenAnonymous() throws Exception {
        mockMvc.perform(get("/api/v1/books"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void listBooks_other_isPublic() throws Exception {
        when(bookRepository.search(any(BookSearchCriteria.class), any(Pageable.class)))
                .thenReturn(Page.empty());
        when(preferencesUseCase.getUserIdsWithPrivateCollection(any()))
                .thenReturn(List.of());

        mockMvc.perform(get("/api/v1/books").param("owner", "other"))
                .andExpect(status().isOk());
    }

    @Test
    void listBooks_all_showsOwnBooks_despitePrivate() throws Exception {
        Book book = sampleBook();
        book.setComment("personal");
        when(bookRepository.search(any(BookSearchCriteria.class), any(Pageable.class)))
                .thenReturn(new org.springframework.data.domain.PageImpl<>(List.of(book)));
        when(preferencesUseCase.getUserIdsWithPrivateCollection(any()))
                .thenReturn(List.of("u1"));

        mockMvc.perform(get("/api/v1/books").with(user("u1")).param("owner", "all"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].comment").value("personal"));
    }

    @Test
    void listBooks_returnsEmptyPage_whenNoBooks() throws Exception {
        when(bookRepository.search(any(BookSearchCriteria.class), any(Pageable.class))).thenReturn(Page.empty());

        mockMvc.perform(get("/api/v1/books").with(user("u1")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content").isEmpty());
    }

    @Test
    void listBooks_filtersByState() throws Exception {
        when(bookRepository.search(any(BookSearchCriteria.class), any(Pageable.class))).thenReturn(Page.empty());

        mockMvc.perform(get("/api/v1/books").with(user("u1")).param("state", "READING"))
                .andExpect(status().isOk());

        ArgumentCaptor<BookSearchCriteria> captor = ArgumentCaptor.forClass(BookSearchCriteria.class);
        verify(bookRepository).search(captor.capture(), any(Pageable.class));
        org.assertj.core.api.Assertions.assertThat(captor.getValue().state()).isEqualTo(BookState.READING);
    }

    @Test
    void listBooks_filtersByGenre() throws Exception {
        when(bookRepository.search(any(BookSearchCriteria.class), any(Pageable.class))).thenReturn(Page.empty());

        mockMvc.perform(get("/api/v1/books").with(user("u1")).param("genre", "fantasía"))
                .andExpect(status().isOk());

        ArgumentCaptor<BookSearchCriteria> captor = ArgumentCaptor.forClass(BookSearchCriteria.class);
        verify(bookRepository).search(captor.capture(), any(Pageable.class));
        org.assertj.core.api.Assertions.assertThat(captor.getValue().genre()).isEqualTo("fantasía");
    }

    @Test
    void listBooks_returns400_whenNameTooLong() throws Exception {
        mockMvc.perform(get("/api/v1/books").with(user("u1")).param("name", "a".repeat(101)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    void listBooks_filtersByNameAuthorAndType() throws Exception {
        when(bookRepository.search(any(BookSearchCriteria.class), any(Pageable.class))).thenReturn(Page.empty());

        mockMvc.perform(get("/api/v1/books").with(user("u1"))
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

        mockMvc.perform(get("/api/v1/books/b1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("b1"))
                .andExpect(jsonPath("$.title").value("Cien años de soledad"))
                .andExpect(jsonPath("$.state").value("TO_READ"));
    }

    @Test
    void getBook_hidesPrivateFields_whenNotOwner() throws Exception {
        Book book = sampleBook();
        book.setComment("muy personal");
        book.setStart(5);
        when(bookRepository.findById("b1")).thenReturn(Optional.of(book));
        when(preferencesUseCase.getUserIdsWithPrivateCollection(CollectionType.BOOKS))
                .thenReturn(List.of("u1"));

        mockMvc.perform(get("/api/v1/books/b1").with(user("other")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Cien años de soledad"))
                .andExpect(jsonPath("$.comment").value(Matchers.nullValue()))
                .andExpect(jsonPath("$.start").value(Matchers.nullValue()));
    }

    @Test
    void getBook_showsPrivateFields_whenCollectionPublic() throws Exception {
        Book book = sampleBook();
        book.setComment("muy personal");
        book.setStart(5);
        when(bookRepository.findById("b1")).thenReturn(Optional.of(book));
        when(preferencesUseCase.getUserIdsWithPrivateCollection(CollectionType.BOOKS))
                .thenReturn(List.of());

        mockMvc.perform(get("/api/v1/books/b1").with(user("other")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.comment").value("muy personal"))
                .andExpect(jsonPath("$.start").value(5));
    }

    @Test
    void getBook_showsPrivateFields_whenOwner() throws Exception {
        Book book = sampleBook();
        book.setComment("muy personal");
        when(bookRepository.findById("b1")).thenReturn(Optional.of(book));

        mockMvc.perform(get("/api/v1/books/b1").with(user("u1")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.comment").value("muy personal"));
    }

    @Test
    void getBook_showsPrivateFields_whenAdmin() throws Exception {
        Book book = sampleBook();
        book.setComment("muy personal");
        when(bookRepository.findById("b1")).thenReturn(Optional.of(book));

        mockMvc.perform(get("/api/v1/books/b1").with(user("admin").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.comment").value("muy personal"));
    }

    @Test
    void listBooks_hidesPrivateFields_whenNotOwner() throws Exception {
        Book book = sampleBook();
        book.setOwnerId("someone-else");
        book.setComment("muy personal");
        when(bookRepository.search(any(BookSearchCriteria.class), any(Pageable.class)))
                .thenReturn(new org.springframework.data.domain.PageImpl<>(List.of(book)));
        when(preferencesUseCase.getUserIdsWithPrivateCollection(CollectionType.BOOKS))
                .thenReturn(List.of("someone-else"));

        mockMvc.perform(get("/api/v1/books").with(user("other")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].comment").value(Matchers.nullValue()));
    }

    @Test
    void getBook_showsOwner_everyone() throws Exception {
        Book book = sampleBook();
        book.setComment("muy personal");
        book.setUserOwned(com.wikicollection.domain.model.UserOwned.builder()
                .ownerId("u1").ownerName("Javi").username("javi").build());
        when(bookRepository.findById("b1")).thenReturn(Optional.of(book));
        when(preferencesUseCase.getUserIdsWithPrivateCollection(CollectionType.BOOKS))
                .thenReturn(List.of("u1"));

        mockMvc.perform(get("/api/v1/books/b1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userOwned.ownerId").value("u1"))
                .andExpect(jsonPath("$.userOwned.ownerName").value("Javi"))
                .andExpect(jsonPath("$.userOwned.username").value("javi"))
                .andExpect(jsonPath("$.comment").value(Matchers.nullValue()));
    }

    @Test
    void getBook_returns404_whenMissing() throws Exception {
        when(bookRepository.findById("nope")).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/v1/books/nope"))
                .andExpect(status().isNotFound());
    }

    @Test
    void createBook_returns201_withLocation() throws Exception {
        when(bookRepository.save(any(Book.class))).thenAnswer(invocation -> {
            Book saved = invocation.getArgument(0);
            saved.setId("b-new");
            return saved;
        });

        mockMvc.perform(post("/api/v1/books").with(user("u1"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":"Cien años de soledad","author":"Gabriel García Márquez","state":"TO_READ","type":"NOVEL"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", Matchers.containsString("/api/v1/books/b-new")))
                .andExpect(jsonPath("$.id").value("b-new"))
                .andExpect(jsonPath("$.title").value("Cien años de soledad"));
    }

    @Test
    void createBook_returns400_whenInvalid() throws Exception {
        mockMvc.perform(post("/api/v1/books").with(user("u1"))
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

        mockMvc.perform(post("/api/v1/books").with(user("u1"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":"Cien años de soledad","author":"Gabriel","externalId":"gb123","state":"TO_READ","type":"NOVEL"}
                                """))
                .andExpect(status().isConflict());
    }

    @Test
    void createBook_returns400_whenStartOutOfRange() throws Exception {
        mockMvc.perform(post("/api/v1/books").with(user("u1"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":"Cien años","author":"G.G.M.","state":"TO_READ","type":"NOVEL","start":9}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createBook_returns400_whenStartDateIsFuture() throws Exception {
        mockMvc.perform(post("/api/v1/books").with(user("u1"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":"Cien años","author":"G.G.M.","state":"TO_READ","type":"NOVEL","startDate":"2099-01-01"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("Fechas mal formadas")));
    }

    @Test
    void createBook_returns400_whenStartAfterEnd() throws Exception {
        mockMvc.perform(post("/api/v1/books").with(user("u1"))
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

        mockMvc.perform(put("/api/v1/books/b1").with(user("u1"))
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

        mockMvc.perform(put("/api/v1/books/b1").with(user("u1"))
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
    void updateBook_updatesPagesRead() throws Exception {
        when(bookRepository.findById("b1")).thenReturn(Optional.of(sampleBook()));
        when(bookRepository.save(any(Book.class))).thenAnswer(invocation -> invocation.getArgument(0));

        mockMvc.perform(put("/api/v1/books/b1").with(user("u1"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":"Dune","author":"Herbert","state":"READING","type":"NOVEL","pages":300,"pagesRead":120}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pagesRead").value(120));
    }

    @Test
    void updateBook_returns400_whenNegativePagesRead() throws Exception {
        mockMvc.perform(put("/api/v1/books/b1").with(user("u1"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":"Dune","author":"Herbert","state":"READING","type":"NOVEL","pagesRead":-5}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getBook_returnsPagesRead() throws Exception {
        Book book = sampleBook();
        book.setPagesRead(100);
        when(bookRepository.findById("b1")).thenReturn(Optional.of(book));

        mockMvc.perform(get("/api/v1/books/b1").with(user("u1")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pagesRead").value(100));
    }

    @Test
    void deleteBook_returns204_whenExists() throws Exception {
        when(bookRepository.findById("b1")).thenReturn(Optional.of(sampleBook()));

        mockMvc.perform(delete("/api/v1/books/b1").with(user("u1")))
                .andExpect(status().isNoContent());

        verify(bookRepository).deleteById("b1");
    }

    @Test
    void deleteBook_returns404_whenMissing() throws Exception {
        when(bookRepository.findById("nope")).thenReturn(Optional.empty());

        mockMvc.perform(delete("/api/v1/books/nope").with(user("u1")))
                .andExpect(status().isNotFound());
    }

    @Test
    void search_returnsGoogleBooksResults() throws Exception {
        BookSearchResult result = new BookSearchResult(
                "abc123", "Cien años de soledad", List.of("Gabriel García Márquez"),
                "9780307474728", "http://thumb", "Sinopsis", 417,
                "Vintage Español", "2011-05-03", "es", List.of("Literatura"));
        when(googleBooksClient.search("cien")).thenReturn(List.of(result));

        mockMvc.perform(get("/api/v1/books/search").param("name", "cien"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].title").value("Cien años de soledad"))
                .andExpect(jsonPath("$.content[0].isbn").value("9780307474728"))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    void search_returns400_whenBlankQuery() throws Exception {
        mockMvc.perform(get("/api/v1/books/search").param("name", " "))
                .andExpect(status().isBadRequest());
    }

    @Test
    void searchByIsbn_returns200() throws Exception {
        BookSearchResult result = new BookSearchResult(
                "abc123", "Dune", List.of("Frank Herbert"),
                "9788498382671", "http://thumb", "Sinopsis", 412,
                "Debolsillo", "2008-01-01", "es", List.of("Novela"));
        when(googleBooksClient.searchByIsbn("9788498382671")).thenReturn(List.of(result));

        mockMvc.perform(get("/api/v1/books/search").param("isbn", "978-84-9838-267-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].title").value("Dune"))
                .andExpect(jsonPath("$.content[0].isbn").value("9788498382671"))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    void search_returns400_whenNoParams() throws Exception {
        mockMvc.perform(get("/api/v1/books/search"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void searchByIsbn_returns400_whenBlank() throws Exception {
        mockMvc.perform(get("/api/v1/books/search").param("isbn", "  "))
                .andExpect(status().isBadRequest());
    }

    @Test
    void search_prefersIsbn_whenBothParams() throws Exception {
        BookSearchResult result = new BookSearchResult(
                "abc123", "Dune", List.of("Frank Herbert"),
                "9788498382671", "http://thumb", "Sinopsis", 412,
                "Debolsillo", "2008-01-01", "es", List.of("Novela"));
        when(googleBooksClient.searchByIsbn("9788498382671")).thenReturn(List.of(result));

        mockMvc.perform(get("/api/v1/books/search").param("isbn", "9788498382671").param("name", "dune"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].isbn").value("9788498382671"))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    void search_returnsEmpty_whenGoogleUnavailable() throws Exception {
        when(googleBooksClient.search("cien")).thenReturn(List.of());

        mockMvc.perform(get("/api/v1/books/search").param("name", "cien"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isEmpty());
    }

    @Test
    void search_returns502_whenGoogleThrowsRestClientError() throws Exception {
        when(googleBooksClient.search("cien")).thenThrow(
                new RestClientResponseException(
                        "error", 500, "Internal Server Error", HttpHeaders.EMPTY, new byte[0], StandardCharsets.UTF_8));

        mockMvc.perform(get("/api/v1/books/search").param("name", "cien"))
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.status").value(502));
    }

    @Test
    void search_returns503_whenGoogleUnreachable() throws Exception {
        when(googleBooksClient.search("cien")).thenThrow(new ResourceAccessException("no disponible"));

        mockMvc.perform(get("/api/v1/books/search").param("name", "cien"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.status").value(503));
    }

    @Test
    void updateProgress_updatesPagesRead() throws Exception {
        Book book = sampleBook();
        book.setId("b1");
        book.setOwnerId("u1");
        book.setPages(300);
        when(bookRepository.findById("b1")).thenReturn(Optional.of(book));
        when(bookRepository.save(any(Book.class))).thenAnswer(invocation -> invocation.getArgument(0));

        mockMvc.perform(patch("/api/v1/books/b1/progress").with(user("u1"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"pagesRead":80}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pagesRead").value(80));
    }

    @Test
    void cors_allowsFrontendOrigin() throws Exception {
        mockMvc.perform(options("/api/v1/books")
                        .header("Origin", "http://localhost:5173")
                        .header("Access-Control-Request-Method", "GET"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:5173"));
    }

    @Test
    void cors_allowsVercelFrontend() throws Exception {
        mockMvc.perform(options("/api/v1/books")
                        .header("Origin", "https://frontend-collection-eta.vercel.app")
                        .header("Access-Control-Request-Method", "GET"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin",
                        "https://frontend-collection-eta.vercel.app"));
    }
}
