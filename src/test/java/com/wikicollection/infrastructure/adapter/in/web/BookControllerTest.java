package com.wikicollection.infrastructure.adapter.in.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
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

import java.util.List;
import java.util.Optional;

import com.wikicollection.domain.model.Book;
import com.wikicollection.domain.model.BookSearchResult;
import com.wikicollection.domain.model.BookStatus;
import com.wikicollection.infrastructure.adapter.out.google.GoogleBooksClient;
import com.wikicollection.infrastructure.adapter.out.persistence.BookEntity;
import com.wikicollection.infrastructure.adapter.out.persistence.SpringDataBookRepository;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(properties = "spring.data.mongodb.auto-index-creation=false")
@AutoConfigureMockMvc
class BookControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private SpringDataBookRepository springDataBookRepository;

    @MockitoBean
    private GoogleBooksClient googleBooksClient;

    private BookEntity sampleEntity() {
        return BookEntity.builder()
                .id("b1")
                .title("Cien años de soledad")
                .authors(List.of("Gabriel García Márquez"))
                .status(BookStatus.WISHLIST)
                .build();
    }

    @Test
    void listBooks_returnsEmptyPage_whenNoBooks() throws Exception {
        when(springDataBookRepository.findAll(any(Pageable.class))).thenReturn(Page.empty());

        mockMvc.perform(get("/api/books"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content").isEmpty());
    }

    @Test
    void listBooks_filtersByStatus() throws Exception {
        when(springDataBookRepository.findByStatus(eq(BookStatus.READING), any(Pageable.class))).thenReturn(Page.empty());

        mockMvc.perform(get("/api/books").param("status", "reading"))
                .andExpect(status().isOk());

        verify(springDataBookRepository).findByStatus(eq(BookStatus.READING), any(Pageable.class));
    }

    @Test
    void listBooks_filtersByTag() throws Exception {
        when(springDataBookRepository.findByTagsContaining(eq("ficcion"), any(Pageable.class))).thenReturn(Page.empty());

        mockMvc.perform(get("/api/books").param("tag", "ficcion"))
                .andExpect(status().isOk());

        verify(springDataBookRepository).findByTagsContaining(eq("ficcion"), any(Pageable.class));
    }

    @Test
    void getBook_returnsBook_whenExists() throws Exception {
        BookEntity entity = sampleEntity();
        when(springDataBookRepository.findById("b1")).thenReturn(Optional.of(entity));

        mockMvc.perform(get("/api/books/b1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("b1"))
                .andExpect(jsonPath("$.title").value("Cien años de soledad"))
                .andExpect(jsonPath("$.status").value("WISHLIST"));
    }

    @Test
    void getBook_returns404_whenMissing() throws Exception {
        when(springDataBookRepository.findById("nope")).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/books/nope"))
                .andExpect(status().isNotFound());
    }

    @Test
    void createBook_returns201_withLocation() throws Exception {
        when(springDataBookRepository.save(any(BookEntity.class))).thenAnswer(invocation -> {
            BookEntity saved = invocation.getArgument(0);
            saved.setId("b-new");
            return saved;
        });

        mockMvc.perform(post("/api/books")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":"Cien años de soledad","authors":["Gabriel García Márquez"],"status":"WISHLIST"}
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
                                {"title":"","authors":[],"status":"WISHLIST"}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createBook_returns409_whenDuplicateIsbn() throws Exception {
        BookEntity existing = BookEntity.builder()
                .id("other")
                .isbn("9780307474728")
                .build();
        when(springDataBookRepository.findByIsbn("9780307474728")).thenReturn(Optional.of(existing));

        mockMvc.perform(post("/api/books")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":"Cien años de soledad","authors":["Gabriel García Márquez"],"status":"WISHLIST","isbn":"9780307474728"}
                                """))
                .andExpect(status().isConflict());
    }

    @Test
    void updateBook_returnsUpdatedBook() throws Exception {
        when(springDataBookRepository.findById("b1")).thenReturn(Optional.of(sampleEntity()));
        when(springDataBookRepository.save(any(BookEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        mockMvc.perform(put("/api/books/b1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":"Nuevo título","authors":["Autor Actualizado"],"status":"COMPLETED"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Nuevo título"))
                .andExpect(jsonPath("$.status").value("COMPLETED"));
    }

    @Test
    void deleteBook_returns204_whenExists() throws Exception {
        when(springDataBookRepository.findById("b1")).thenReturn(Optional.of(sampleEntity()));

        mockMvc.perform(delete("/api/books/b1"))
                .andExpect(status().isNoContent());

        verify(springDataBookRepository).deleteById("b1");
    }

    @Test
    void deleteBook_returns404_whenMissing() throws Exception {
        when(springDataBookRepository.findById("nope")).thenReturn(Optional.empty());

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

        mockMvc.perform(get("/api/books/search").param("q", "cien"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].title").value("Cien años de soledad"))
                .andExpect(jsonPath("$[0].isbn").value("9780307474728"));
    }

    @Test
    void search_returns400_whenBlankQuery() throws Exception {
        mockMvc.perform(get("/api/books/search").param("q", " "))
                .andExpect(status().isBadRequest());
    }

    @Test
    void search_returns500_whenGoogleUnavailable() throws Exception {
        when(googleBooksClient.search("cien")).thenThrow(new IllegalStateException("Google Books API no disponible"));

        mockMvc.perform(get("/api/books/search").param("q", "cien"))
                .andExpect(status().isInternalServerError());
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